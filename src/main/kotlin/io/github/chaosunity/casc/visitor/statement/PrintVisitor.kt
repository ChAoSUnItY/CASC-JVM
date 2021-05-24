package io.github.chaosunity.casc.visitor.statement

import io.github.chaosunity.casc.CASCBaseVisitor
import io.github.chaosunity.casc.CASCParser
import io.github.chaosunity.casc.parsing.node.statement.OutputStatement
import io.github.chaosunity.casc.parsing.node.statement.PrintStatement
import io.github.chaosunity.casc.parsing.node.statement.PrintlnStatement
import io.github.chaosunity.casc.visitor.expression.ExpressionVisitor

class PrintVisitor(private val ev: ExpressionVisitor) : CASCBaseVisitor<OutputStatement<*>>() {
    override fun visitPrintStatement(ctx: CASCParser.PrintStatementContext): OutputStatement<*> =
        PrintStatement(ctx.findExpression()!!.accept(ev))

    override fun visitPrintlnStatement(ctx: CASCParser.PrintlnStatementContext): OutputStatement<*> =
        PrintlnStatement(ctx.findExpression()!!.accept(ev))
}