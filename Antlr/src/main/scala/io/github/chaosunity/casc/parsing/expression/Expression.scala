package io.github.chaosunity.casc.parsing.expression

import io.github.chaosunity.casc.parsing.`type`.Type
import io.github.chaosunity.casc.parsing.statement.Statement

abstract class Expression(val `type`: Type) extends Statement {
}
