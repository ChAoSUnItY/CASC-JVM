package org.yakou.lang.bind

enum class PrimitiveType(
    val typeLiteral: String,
    val jvmClazz: Class<*>,
    val wrappedJvmClazz: Class<*>,
    val descriptor: String,
    val precedence: Int
) {
    Unit("unit", Void.TYPE, Void::class.java, "V", -1),
    Bool("bool", java.lang.Boolean.TYPE, java.lang.Boolean::class.java, "Z", 0),
    Char("char", Character.TYPE, Character::class.java, "C", 1),
    I8("i8", java.lang.Byte.TYPE, java.lang.Byte::class.java, "B", 2),
    I16("i16", java.lang.Short.TYPE, java.lang.Short::class.java, "S", 2),
    I32("i32", Integer.TYPE, Integer::class.java, "I", 2),
    I64("i64", java.lang.Long.TYPE, java.lang.Long::class.java, "J", 3),
    F32("f32", java.lang.Float.TYPE, java.lang.Float::class.java, "F", 4),
    F64("f64", java.lang.Double.TYPE, java.lang.Double::class.java, "D", 5),
    Str("str", java.lang.String::class.java, java.lang.String::class.java, "Ljava/lang/String;", -1),
    ;

    companion object {
        private val values: Array<PrimitiveType> = values()
        val primitiveTypes: Array<PrimitiveType> = arrayOf(Unit, Bool, Char, I8, I16, I32, I64, F32, F64)
        private val numberTypes: Array<PrimitiveType> = arrayOf(I8, I16, I32, I64, F32, F64)
        private val integerTypes: Array<PrimitiveType> = arrayOf(I8, I16, I32, I64)
        private val floatTypes: Array<PrimitiveType> = arrayOf(F32, F64)

        fun isPrimitiveType(typeLiteral: String): Boolean =
            values.any { it.typeLiteral == typeLiteral }

        fun isPrimitiveType(primitiveType: PrimitiveType): Boolean =
            values.any { it == primitiveType }

        fun findPrimitiveType(typeLiteral: String): PrimitiveType? =
            values.find { it.typeLiteral == typeLiteral }

        fun isNumberType(typeLiteral: String): Boolean =
            numberTypes.any { it.typeLiteral == typeLiteral }

        fun isNumberType(primitiveType: PrimitiveType): Boolean =
            numberTypes.any { it == primitiveType }

        fun isIntegerType(typeLiteral: String): Boolean =
            integerTypes.any { it.typeLiteral == typeLiteral }

        fun isIntegerType(primitiveType: PrimitiveType): Boolean =
            integerTypes.any { it == primitiveType }

        fun isFloatType(typeLiteral: String): Boolean =
            floatTypes.any { it.typeLiteral == typeLiteral }

        fun isFloatType(primitiveType: PrimitiveType): Boolean =
            floatTypes.any { it == primitiveType }

        fun fromClass(clazz: Class<*>): TypeInfo? = when {
            clazz.isPrimitive -> TypeInfo.Primitive(primitiveTypes.find { it.jvmClazz == clazz || it.wrappedJvmClazz == clazz }!!)
            clazz.typeName == "java.lang.String" -> TypeInfo.Primitive(Str)
            else -> null
        }
    }
}