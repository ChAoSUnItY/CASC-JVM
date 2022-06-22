package org.yakou.lang.checker

import chaos.unity.nenggao.Span
import com.diogonunes.jcolor.Ansi
import com.diogonunes.jcolor.Attribute
import org.yakou.lang.ast.*
import org.yakou.lang.bind.PrimitiveType
import org.yakou.lang.bind.TypeInfo
import org.yakou.lang.compilation.CompilationUnit
import org.yakou.lang.util.SpanHelper
import org.yakou.lang.util.colorize

class Checker(private val compilationUnit: CompilationUnit) {
    fun check() {
        checkYkFile(compilationUnit.ykFile!!)
    }

    fun checkYkFile(ykFile: YkFile) {
        for (item in ykFile.items)
            checkItem(item)
    }

    private fun checkItem(item: Item) {
        when (item) {
            is Item.Package -> {
                if (item.items != null)
                    for (innerItem in item.items)
                        checkItem(innerItem)
            }
            is Item.Const -> checkConst(item)
            is Item.StaticField -> checkStaticField(item)
            is Item.Class -> checkClass(item)
            is Item.Function -> checkFunction(item)
        }
    }

    private fun checkConst(const: Item.Const) {
        // Check constant has illegal modifiers
        if (const.modifiers.hasModifier(Modifier.Mut)) {
            reportIllegalMut(const.modifiers[Modifier.Mut]!!, "Constant cannot be mutable")
        }
        // Check expression
        checkExpression(const.expression)
        // Check expression's type explicitly matches constant's explicit type
        if (const.typeInfo != const.expression.finalType) {
            val explicitTypeLiteral =
                if (compilationUnit.preference.enableColor) Ansi.colorize(
                    const.typeInfo.toString(),
                    Attribute.CYAN_TEXT()
                )
                else const.typeInfo.toString()
            val expressionTypeLiteral =
                if (compilationUnit.preference.enableColor) Ansi.colorize(
                    const.expression.finalType.toString(),
                    Attribute.RED_TEXT()
                )
                else const.expression.finalType.toString()

            compilationUnit.reportBuilder
                .error(
                    SpanHelper.expandView(const.span, compilationUnit.maxLineCount),
                    "Cannot implicitly cast constant's expression"
                )
                .label(const.type.span, "Expression type should explicitly be `$explicitTypeLiteral`")
                .color(Attribute.CYAN_TEXT())
                .build()
                .label(const.expression.span, "Expression has type `$expressionTypeLiteral`")
                .color(Attribute.RED_TEXT())
                .build().build()
        }
    }

    private fun checkStaticField(staticField: Item.StaticField) {
        // Check expression
        checkExpression(staticField.expression)
        // Check if expression is not castable to explicit type
        if (staticField.expression.finalType is TypeInfo.Primitive)
        // Check if it's applicable to convert static field into constant
        if (staticField.expression.finalType is TypeInfo.Primitive &&
            staticField.expression is Expression.LiteralExpression &&
            !staticField.modifiers.hasModifier(Modifier.Mut)) {
            val staticLiteral =
                if (compilationUnit.preference.enableColor) Ansi.colorize(
                    "static",
                    Attribute.CYAN_TEXT()
                )
                else "static"
            val constLiteral =
                if (compilationUnit.preference.enableColor) Ansi.colorize(
                    "const",
                    Attribute.CYAN_TEXT()
                )
                else "const"
            val expressionTypeLiteral =
                if (compilationUnit.preference.enableColor) Ansi.colorize(
                    staticField.expression.finalType.toString(),
                    Attribute.GREEN_TEXT()
                )
                else staticField.expression.finalType.toString()

            compilationUnit.reportBuilder
                .warning(SpanHelper.expandView(staticField.span, compilationUnit.maxLineCount), "Static field is sufficient to convert into constant")
                .label(staticField.static.span, "Consider replace `$staticLiteral` with `$constLiteral`")
                .color(Attribute.YELLOW_TEXT())
                .build()
                .label(staticField.expression.span, "Expression has primitive type `$expressionTypeLiteral`")
                .color(Attribute.CYAN_TEXT())
                .build().build()
        }
    }

    private fun checkClass(function: Item.Class) {
        if (function.classItems != null)
            for (classItem in function.classItems)
                checkClassItem(classItem)
    }

    private fun checkClassItem(classItem: ClassItem) {
        when (classItem) {
            is ClassItem.Field -> checkField(classItem)
        }
    }

    private fun checkField(field: ClassItem.Field) {

    }

    private fun checkFunction(function: Item.Function) {
        // Check top-level function has illegal modifiers
        if (function.functionInstance.ownerTypeInfo is TypeInfo.PackageClass && function.modifiers.hasModifier(Modifier.Mut)) {
            reportIllegalMut(function.modifiers[Modifier.Mut]!!, "Top-level function cannot be mutable")
        }
    }

    private fun checkExpression(expression: Expression) {
        when (expression) {
            is Expression.NumberLiteral -> checkNumberLiteral(expression)
            Expression.Undefined -> TODO("UNREACHABLE")
        }
    }

    private fun checkNumberLiteral(numberLiteral: Expression.NumberLiteral) {
        val isIntegerValue = numberLiteral.value.toInt().toDouble() != numberLiteral.value

        if (numberLiteral.dot != null && numberLiteral.floatPart != null && numberLiteral.specifiedType != null) {
            if (isIntegerValue) {
                // Number literal represents a float number but specified with an integer type
                val specifiedTypeLiteral =
                    if (compilationUnit.preference.enableColor) Ansi.colorize(
                        numberLiteral.specifiedType.standardizeType(),
                        Attribute.RED_TEXT()
                    )
                    else numberLiteral.specifiedType.standardizeType()

                compilationUnit.reportBuilder
                    .error(
                        SpanHelper.expandView(numberLiteral.span, compilationUnit.maxLineCount),
                        "Illegal type suffix `$specifiedTypeLiteral` for float number literal"
                    )
                    .label(numberLiteral.floatPart.span, "Float number literal here")
                    .color(Attribute.CYAN_TEXT())
                    .build()
                    .label(numberLiteral.specifiedType.span, "Illegal type suffix here")
                    .color(Attribute.RED_TEXT())
                    .build().build()
            } else if (numberLiteral.originalType is TypeInfo.Primitive && (numberLiteral.originalType as TypeInfo.Primitive).type == PrimitiveType.F64) {
                // Redundant type suffix `f64` for float number literal
                val specifiedTypeLiteral =
                    if (compilationUnit.preference.enableColor) Ansi.colorize(
                        numberLiteral.specifiedType.standardizeType(),
                        Attribute.CYAN_TEXT()
                    )
                    else numberLiteral.specifiedType.standardizeType()

                compilationUnit.reportBuilder
                    .warning(
                        SpanHelper.expandView(numberLiteral.span, compilationUnit.maxLineCount),
                        "Redundant type suffix `$specifiedTypeLiteral` for float number literal"
                    )
                    .label(numberLiteral.floatPart.span, "Float number literal here")
                    .color(Attribute.CYAN_TEXT())
                    .build()
                    .label(numberLiteral.specifiedType.span, "Redundant type suffix here")
                    .color(Attribute.YELLOW_TEXT())
                    .build().build()
            }
        }
    }

    private fun reportIllegalMut(span: Span, labelMessage: String) {
        val mutLiteral = colorize("mut", compilationUnit, Attribute.RED_TEXT())

        compilationUnit.reportBuilder
            .error(SpanHelper.expandView(span, compilationUnit.maxLineCount), "Illegal modifier `$mutLiteral`")
            .label(span, labelMessage)
            .color(Attribute.RED_TEXT())
            .build().build()
    }
}