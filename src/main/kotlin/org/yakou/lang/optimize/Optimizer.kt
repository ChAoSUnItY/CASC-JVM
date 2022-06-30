package org.yakou.lang.optimize

import org.yakou.lang.ast.*
import org.yakou.lang.compilation.CompilationUnit

class Optimizer(private val compilationUnit: CompilationUnit) {
    fun optimize() {
        optimizeYkFile(compilationUnit.ykFile!!)
    }

    private fun optimizeYkFile(ykFile: YkFile) {
        for (item in ykFile.items)
            optimizeItem(item)
    }

    private fun optimizeItem(item: Item) {
        when (item) {
            is Item.Class -> TODO()
            is Item.Const -> TODO()
            is Item.Function -> TODO()
            is Item.Package -> {
                if (item.items != null)
                    for (innerItem in item.items)
                        optimizeItem(innerItem)
            }
            is Item.StaticField -> TODO()
        }
    }

    private fun optimizeClass(clazz: Item.Class) {
        if (clazz.classItems != null)
            for (classItem in clazz.classItems)
                optimizeClassItem(classItem)
    }

    private fun optimizeClassItem(classItem: ClassItem) {
        when (classItem) {
            is ClassItem.Field -> TODO()
        }
    }

    private fun optimizeConst(const: Item.Const) {

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
            is FunctionBody.SingleExpression -> optimizeExpression(functionBody.expression)
        }
    }

    private fun optimizeStaticField(staticField: Item.StaticField) {

    }

    private fun optimizeStatement(statement: Statement) {
    }

    private fun optimizeExpression(expression: Expression) {
        when (expression) {
            is Expression.NumberLiteral -> optimizeNumberLiteral(expression)
            Expression.Undefined -> {} // Cannot be optimized
        }
    }

    private fun optimizeNumberLiteral(numberLiteral: Expression.NumberLiteral) {
        // ???
    }
}