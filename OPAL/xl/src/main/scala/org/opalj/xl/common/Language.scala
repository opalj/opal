/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package common

import org.opalj.xl.coordinator.Coordinator.V

object Language extends Enumeration {
    type Language = Value

    val Unknown, JavaScript, Java, Native, WebAssembly, Python, TIP = Value
}

object Constants {
    val jsrResultVariableName = "JSResultVariable";
}

object VarNames {
    var i = 0
    def genVName(): String = {
        i = i + 1;
        s"v$i"
    }
}

case class ForeignFunctionCall(functionName: String, actualParams: List[(String, V)], returnValue: V)
