package io.github.chaosunity.casc.bytecode.expression

import io.github.chaosunity.casc.parsing.node.expression.*
import io.github.chaosunity.casc.parsing.node.expression.ArithmeticExpression.*
import io.github.chaosunity.casc.parsing.scope.Scope
import jdk.internal.org.objectweb.asm.MethodVisitor


class ExpressionFactory(mv: MethodVisitor, scope: Scope) {
    private val vrf = VariableReferenceFactory(mv, scope)
    private val vf = ValueFactory(mv)
    private val pf = ParameterFactory(mv, scope)
    private val conditionalFactory = ConditionalFactory(this, mv)
    private val callFactory = CallFactory(this, scope, mv)
    private val af = ArithmeticFactory(this, mv)

    fun generate(expression: Expression<*>) =
        when (expression) {
            is VariableReference -> generate(expression)
            is Parameter -> generate(expression)
            is Value -> generate(expression)
            is ConstructorCall -> generate(expression)
            is SuperCall -> generate(expression)
            is FunctionCall -> generate(expression)
            is Addition -> generate(expression)
            is Subtraction -> generate(expression)
            is Multiplication -> generate(expression)
            is Division -> generate(expression)
            is Conditional -> generate(expression)
            is EmptyExpression -> {}
            else -> throw RuntimeException("Invalid expression")
        }

    fun generate(variableReference: VariableReference) =
        vrf.generate(variableReference)

    fun generate(parameter: Parameter) =
        pf.generate(parameter)

    fun generate(value: Value) =
        vf.generate(value)

    fun generate(constructorCall: ConstructorCall) =
        callFactory.generate(constructorCall)

    fun generate(superCall: SuperCall) =
        callFactory.generate(superCall)

    fun generate(functionCall: FunctionCall) =
        callFactory.generate(functionCall)

    fun generate(expression: Addition) =
        af.generate(expression)

    fun generate(expression: Subtraction) =
        af.generate(expression)

    fun generate(expression: Multiplication) =
        af.generate(expression)

    fun generate(expression: Division) =
        af.generate(expression)

    fun generate(conditional: Conditional) =
        conditionalFactory.generate(conditional)
}