package org.yakou.lang.optimizer

import chaos.unity.nenggao.Span
import org.yakou.lang.ast.*
import org.yakou.lang.bind.ClassMember
import org.yakou.lang.bind.TypeInfo
import org.yakou.lang.bind.Variable
import org.yakou.lang.compilation.CompilationUnit

class Optimizer(val compilationUnit: CompilationUnit) {
    fun optimize() {
        optimizeYkFile(compilationUnit.ykFile!!)
    }

    private fun optimizeYkFile(ykFile: YkFile) {
        for (item in ykFile.items)
            optimizeItem(item)
    }

    private fun optimizeItem(item: Item) {
        when (item) {
            is Item.Class -> optimizeClass(item)
            is Item.Const -> optimizeConst(item)
            is Item.Function -> optimizeFunction(item)
            is Item.Package -> {
                if (item.items != null)
                    for (innerItem in item.items)
                        optimizeItem(innerItem)
            }

            is Item.StaticField -> optimizeStaticField(item)
        }
    }

    private fun optimizeClass(clazz: Item.Class) {
        if (clazz.classItems != null)
            for (classItem in clazz.classItems)
                optimizeClassItem(classItem)

        // primary constructor does not need to optimize
    }

    private fun optimizeClassItem(classItem: ClassItem) {
        when (classItem) {
            is ClassItem.Field -> {
                if (classItem.expression != null)
                    classItem.expression = optimizeExpression(classItem.expression!!)
            }
        }
    }

    private fun optimizeConst(const: Item.Const) {
        const.expression = optimizeExpression(const.expression)
    }

    private fun optimizeFunction(function: Item.Function) {
        if (function.body != null)
            optimizeFunctionBody(function.body)
    }

    private fun optimizeFunctionBody(functionBody: FunctionBody) {
        when (functionBody) {
            is FunctionBody.BlockExpression -> {
                for (statement in functionBody.statements)
                    optimizeStatement(statement)
            }

            is FunctionBody.SingleExpression -> {
                functionBody.expression = optimizeExpression(functionBody.expression)
            }
        }
    }

    private fun optimizeStaticField(staticField: Item.StaticField) {
        staticField.expression = optimizeExpression(staticField.expression)
    }

    private fun optimizeStatement(statement: Statement) {
        when (statement) {
            is Statement.VariableDeclaration -> optimizeVariableDeclaration(statement)
            is Statement.For -> optimizeFor(statement)
            is Statement.Block -> optimizeBlock(statement)
            is Statement.Return -> optimizeReturn(statement)
            is Statement.ExpressionStatement -> {
                statement.expression = optimizeExpression(statement.expression)
            }
        }
    }

    private fun optimizeVariableDeclaration(statement: Statement.VariableDeclaration) {
        statement.expression = optimizeExpression(statement.expression)

        if (statement.expression is Expression.LiteralExpression) {
            // can be propagated to other expressions
            statement.variableInstance.propagatable = true
            statement.variableInstance.propagateExpression = statement.expression
        }
    }

    private fun optimizeFor(statement: Statement.For) {
        statement.conditionExpression = optimizeExpression(statement.conditionExpression)
        optimizeBlock(statement.block)
    }

    private fun optimizeBlock(statement: Statement.Block) {
        for (innerStatement in statement.statements) {
            optimizeStatement(innerStatement)
        }
    }

    private fun optimizeReturn(statement: Statement.Return) {
        statement.expression = optimizeExpression(statement.expression)
    }

    private fun optimizeExpression(expression: Expression): Expression = when (expression) {
        is Expression.BinaryExpression -> optimizeBinaryExpression(expression)
        is Expression.Identifier -> optimizeIdentifier(expression)
        is Expression.As -> optimizeAs(expression)
        is Expression.Parenthesized -> optimizeParenthesized(expression)
        is Expression.BoolLiteral -> expression
        is Expression.NumberLiteral -> expression
        is Expression.Empty -> expression
        Expression.Undefined -> expression
    }

    private fun optimizeBinaryExpression(expression: Expression.BinaryExpression): Expression {
        var finalExpression: Expression = expression
        val optimizedLeftExpression = optimizeExpression(expression.leftExpression)
        val optimizedRightExpression = optimizeExpression(expression.rightExpression)

        when (expression.operation) {
            Expression.BinaryExpression.BinaryOperation.Addition,
            Expression.BinaryExpression.BinaryOperation.Subtraction,
            Expression.BinaryExpression.BinaryOperation.Multiplication,
            Expression.BinaryExpression.BinaryOperation.Division,
            Expression.BinaryExpression.BinaryOperation.Modulo -> {
                if (optimizedLeftExpression is Expression.NumberLiteral && optimizedRightExpression is Expression.NumberLiteral)
                    finalExpression = syntheticNumberLiteral(
                        expression.operation.getArithmeticFunctor()!!(
                            optimizedLeftExpression.value,
                            optimizedRightExpression.value
                        ),
                        expression.span,
                        expression.finalType
                    )
            }

            Expression.BinaryExpression.BinaryOperation.LeftShift,
            Expression.BinaryExpression.BinaryOperation.RightShift,
            Expression.BinaryExpression.BinaryOperation.UnsignedRightShift -> {
                if (optimizedLeftExpression is Expression.NumberLiteral && optimizedRightExpression is Expression.NumberLiteral) {
                    finalExpression = syntheticNumberLiteral(
                        expression.operation.getBitwiseFunctor()!!(
                            optimizedLeftExpression.value.toLong(),
                            optimizedRightExpression.value.toInt()
                        ).toDouble(),
                        expression.span,
                        expression.finalType
                    )
                }
            }

            Expression.BinaryExpression.BinaryOperation.LogicalOr,
            Expression.BinaryExpression.BinaryOperation.LogicalAnd -> {
                if (optimizedLeftExpression is Expression.BoolLiteral && optimizedRightExpression is Expression.BoolLiteral) {
                    finalExpression = syntheticBoolLiteral(
                        expression.operation.getLogicalFunctor()!!(
                            optimizedLeftExpression.value,
                            optimizedRightExpression.value
                        ),
                        expression.span
                    )
                }
            }

            Expression.BinaryExpression.BinaryOperation.Equal,
            Expression.BinaryExpression.BinaryOperation.NotEqual,
            Expression.BinaryExpression.BinaryOperation.ExactEqual,
            Expression.BinaryExpression.BinaryOperation.ExactNotEqual -> {
                if (optimizedLeftExpression.finalType != optimizedRightExpression.finalType) {
                    // Types unmatched
                    finalExpression = syntheticBoolLiteral(
                        false,
                        expression.span
                    )
                } else if (optimizedLeftExpression is Expression.NumberLiteral && optimizedRightExpression is Expression.NumberLiteral) {
                    finalExpression = when (expression.operation) {
                        Expression.BinaryExpression.BinaryOperation.Equal,
                        Expression.BinaryExpression.BinaryOperation.ExactEqual ->
                            syntheticBoolLiteral(
                                optimizedLeftExpression.value == optimizedRightExpression.value,
                                expression.span
                            )

                        Expression.BinaryExpression.BinaryOperation.NotEqual,
                        Expression.BinaryExpression.BinaryOperation.ExactNotEqual ->
                            syntheticBoolLiteral(
                                optimizedLeftExpression.value != optimizedRightExpression.value,
                                expression.span
                            )

                        else -> finalExpression
                    }
                } else if (optimizedLeftExpression is Expression.BoolLiteral && optimizedRightExpression is Expression.BoolLiteral) {
                    finalExpression = when (expression.operation) {
                        Expression.BinaryExpression.BinaryOperation.Equal,
                        Expression.BinaryExpression.BinaryOperation.ExactEqual ->
                            syntheticBoolLiteral(
                                optimizedLeftExpression.value == optimizedRightExpression.value,
                                expression.span
                            )

                        Expression.BinaryExpression.BinaryOperation.NotEqual,
                        Expression.BinaryExpression.BinaryOperation.ExactNotEqual ->
                            syntheticBoolLiteral(
                                optimizedLeftExpression.value != optimizedRightExpression.value,
                                expression.span
                            )

                        else -> finalExpression
                    }
                }
            }
            Expression.BinaryExpression.BinaryOperation.Greater,
            Expression.BinaryExpression.BinaryOperation.GreaterEqual,
            Expression.BinaryExpression.BinaryOperation.Lesser,
            Expression.BinaryExpression.BinaryOperation.LesserEqual -> {
                // Unsupported
//                if (optimizedLeftExpression is Expression.NumberLiteral && optimizedRightExpression is Expression.NumberLiteral) {
//                    finalExpression = syntheticNumberLiteral(
//                        expression.operation.get()!!(
//                            optimizedLeftExpression.value.toLong(),
//                            optimizedRightExpression.value.toInt()
//                        ).toDouble(),
//                        expression.span,
//                        expression.finalType
//                    )
//                }
            }
        }

        if (finalExpression is Expression.BinaryExpression) {
            // Unoptimized but lhs and rhs might be optimized, thus we have to update lhs and rhs
            finalExpression.leftExpression = optimizedLeftExpression
            finalExpression.rightExpression = optimizedRightExpression
        }

        return finalExpression
    }

    private fun optimizeIdentifier(expression: Expression.Identifier): Expression {
        val symbolInstance = expression.symbolInstance

        return if (symbolInstance is Variable) {
            if (symbolInstance.propagatable) {
                symbolInstance.dereference()

                symbolInstance.propagateExpression
            } else expression
        } else if (symbolInstance is ClassMember.Field) {
            // Propagate expression when applicable

            if (symbolInstance.isConst) {
                // Inline value without side effect
                symbolInstance.propagateExpression!!
            } else if (symbolInstance.isStatic && (symbolInstance.inline || !symbolInstance.mutable)) {
                // Inline when conditions met
                // - static final
                // - or force inline
                if (symbolInstance.propagateExpression is Expression.LiteralExpression) {
                    symbolInstance.propagateExpression
                } else expression
            } else expression
        } else expression
    }

    private fun optimizeAs(expression: Expression.As): Expression {
        expression.expression = optimizeExpression(expression.expression)

        val innerExpression = expression.expression

        if (innerExpression is Expression.LiteralExpression && expression.finalType is TypeInfo.Primitive) {
            // Do not optimize boxing process!
            return when (innerExpression) {
                is Expression.BoolLiteral -> syntheticBoolLiteral(innerExpression.value, expression.span)
                is Expression.NumberLiteral -> syntheticNumberLiteral(
                    innerExpression.value,
                    expression.span,
                    expression.finalType
                )
            }
        }

        return expression // TODO: Optimize?
    }

    private fun optimizeParenthesized(expression: Expression.Parenthesized): Expression {
        expression.expression = optimizeExpression(expression.expression)

        return expression
    }

    private fun syntheticBoolLiteral(value: Boolean, span: Span): Expression.BoolLiteral {
        val syntheticBoolLiteral = Expression.BoolLiteral(null, span)

        syntheticBoolLiteral.value = value

        return syntheticBoolLiteral
    }

    private fun syntheticNumberLiteral(value: Double, span: Span, finalType: TypeInfo): Expression.NumberLiteral {
        val syntheticNumberLiteral =
            Expression.NumberLiteral(null, null, null, null, span)

        syntheticNumberLiteral.value = value
        syntheticNumberLiteral.specifiedTypeInfo = finalType as TypeInfo.Primitive
        syntheticNumberLiteral.originalType = finalType
        syntheticNumberLiteral.finalType = finalType

        return syntheticNumberLiteral
    }
}