/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package utility

import Coordinator.V

import org.opalj.br.FieldType

object Language extends Enumeration {
    type Language = Value

    val Unknown, JavaScript, Java, Native, WebAssembly, Python, TIP = Value
}

object Constants {
    val javaScriptResultVariableName = "JSResultVariable";
}

object VarNames {
    var i = 0
    def genVName(): String = {
        i = i + 1;
        s"tmpVariableOPAL$i"
    }
}

case class JavaScriptFunctionCall(functionName: String, actualParams: Map[String, (FieldType, Set[AnyRef], Option[Double])], returnValue: V)

case class NativeFunctionCall(functionName: String, actualParams: Map[String, (FieldType, Set[AnyRef])], returnValue: V)