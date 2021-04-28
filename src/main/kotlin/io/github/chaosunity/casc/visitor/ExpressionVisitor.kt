package io.github.chaosunity.casc.visitor

import io.github.chaosunity.antlr.CASCBaseVisitor
import io.github.chaosunity.antlr.CASCParser
import io.github.chaosunity.casc.exception.FunctionNameSameAsClassNameException
import io.github.chaosunity.casc.parsing.LogicalOp
import io.github.chaosunity.casc.parsing.expression.*
import io.github.chaosunity.casc.parsing.expression.math.ArithmeticExpression.*
import io.github.chaosunity.casc.parsing.scope.Scope
import io.github.chaosunity.casc.parsing.type.BuiltInType
import io.github.chaosunity.casc.parsing.type.ClassType
import io.github.chaosunity.casc.util.TypeResolver

class ExpressionVisitor(private val scope: Scope) : CASCBaseVisitor<Expression>() {
    override fun visitVarRef(ctx: CASCParser.VarRefContext?): Expression {
        val variableName = ctx?.text
        val localVariableDeclaration = scope.getLocalVariable(variableName)

        return VarReference(localVariableDeclaration.type(), variableName, ctx?.NEG != null)
    }

    override fun visitVal(ctx: CASCParser.ValContext?): Expression {
        val value = ctx?.value()?.text
        val type = TypeResolver.getFromValue(value)

        return Value(type, value, ctx?.NEG != null)
    }

    override fun visitFunctionCall(ctx: CASCParser.FunctionCallContext?): Expression {
        val functionName = ctx?.functionName()?.text

        if (functionName.equals(scope.className())) {
            throw FunctionNameSameAsClassNameException(functionName!!)
        }

        val argumentCtx = ctx?.argument() ?: listOf()
        val arguments = getArguments(argumentCtx, functionName)
        val signature = scope.getMethodCallSignature(functionName, arguments)

        if (ctx?.owner != null) {
            val owner = ctx.owner.accept(this)

            return FunctionCall(signature, arguments, owner, false)
        }

        val thisType = ClassType(scope.className())

        return FunctionCall(signature, arguments, VarReference(thisType, "this", false), ctx?.NEG != null)
    }

    override fun visitConstructorCall(ctx: CASCParser.ConstructorCallContext?): Expression {
        val className = ctx?.className()?.text
        val argumentCtx = ctx?.argument() ?: listOf()
        val arguments = getArguments(argumentCtx, className)

        return ConstructorCall(className, arguments)
    }

    override fun visitSuperCall(ctx: CASCParser.SuperCallContext?): Expression {
        val argumentCtx = ctx?.argument() ?: listOf()
        val arguments = getArguments(argumentCtx, SuperCall.SUPER_IDENTIFIER())

        return SuperCall(arguments)
    }

    override fun visitModAdd(ctx: CASCParser.ModAddContext?): Expression {
        val left = ctx?.expression(0)?.accept(this)
        val right = ctx?.expression(1)?.accept(this)

        return Addition(left, right, ctx?.NEG != null)
    }

    override fun visitAdd(ctx: CASCParser.AddContext?): Expression {
        val left = ctx?.expression(0)?.accept(this)
        val right = ctx?.expression(1)?.accept(this)

        return Addition(left, right, false)
    }

    override fun visitModSubtract(ctx: CASCParser.ModSubtractContext?): Expression {
        val left = ctx?.expression(0)?.accept(this)
        val right = ctx?.expression(1)?.accept(this)

        return Addition(left, right, ctx?.NEG != null)
    }

    override fun visitSubtract(ctx: CASCParser.SubtractContext?): Expression {
        val left = ctx?.expression(0)?.accept(this)
        val right = ctx?.expression(1)?.accept(this)

        return Subtraction(left, right, false)
    }

    override fun visitModMultiply(ctx: CASCParser.ModMultiplyContext?): Expression {
        val left = ctx?.expression(0)?.accept(this)
        val right = ctx?.expression(1)?.accept(this)

        return Addition(left, right, ctx?.NEG != null)
    }

    override fun visitMultiply(ctx: CASCParser.MultiplyContext?): Expression {
        val left = ctx?.expression(0)?.accept(this)
        val right = ctx?.expression(1)?.accept(this)

        return Multiplication(left, right, false)
    }

    override fun visitModDivide(ctx: CASCParser.ModDivideContext?): Expression {
        val left = ctx?.expression(0)?.accept(this)
        val right = ctx?.expression(1)?.accept(this)

        return Addition(left, right, ctx?.NEG != null)
    }

    override fun visitDivide(ctx: CASCParser.DivideContext?): Expression {
        val left = ctx?.expression(0)?.accept(this)
        val right = ctx?.expression(1)?.accept(this)

        return Division(left, right, false)
    }

    override fun visitIfExpr(ctx: CASCParser.IfExprContext?): Expression {
        val conditionExpressionCtx = ctx?.condition
        val condition = conditionExpressionCtx?.accept(this)
        val trueExpression = ctx?.trueExpression?.accept(this)
        val falseExpression = ctx?.falseExpression?.accept(this)

        return IfExpression(condition, trueExpression, falseExpression)
    }

    override fun visitConditionalExpression(ctx: CASCParser.ConditionalExpressionContext?): Expression {
        val left = ctx?.expression(0)?.accept(this)
        val right = if (ctx?.expression(1) != null) ctx.expression(1)?.accept(this)
        else Value(BuiltInType.INT(), "0", false)
        val logicalOp =
            if (ctx?.cmp != null) LogicalOp.enumSet().find { it.isAlias(ctx.cmp.text) } else LogicalOp.NOT_EQ()

        return ConditionalExpression(left, right, logicalOp)
    }

    private fun getArguments(ctx: List<CASCParser.ArgumentContext?>, identifier: String?): List<Expression?> {
        val signature = scope.getMethodCallSignature(identifier, ctx.map { it?.accept(this) })

        return ctx.sortedWith(Comparator { o1, o2 ->
            if (o1?.name() == null || o2?.name() == null) return@Comparator 0

            val argName1 = o1.name().text
            val argName2 = o2.name().text

            signature.getIndexOfParameter(argName1) - signature.getIndexOfParameter(argName2)
        }).map { it?.expression()?.accept(this) }
    }
}