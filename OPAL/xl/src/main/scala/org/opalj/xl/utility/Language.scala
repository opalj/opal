/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package utility

import Coordinator.V

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.br.FieldType
import org.opalj.tac.fpcf.properties.TheTACAI

object Language extends Enumeration {
    type Language = Value

    val Unknown, JavaScript, Java, Native, WebAssembly, Python = Value
}

object Constants {
    val javaScriptResultVariableName = "JSResultVariable";
}

object VarNames {
    var i = 0
    def genVName(n: Int): String = s"tmpVariableOPAL$n"
}

case class JavaScriptFunctionCall[ContextType, PointsToSet](functionName: String = "", actualParams: Map[(Integer, ContextType, IntTrieSet, TheTACAI), PointsToSet] = Map.empty[(Integer, ContextType, IntTrieSet, TheTACAI), PointsToSet], returnValue: V = null)

case class NativeFunctionCall(functionName: String, actualParams: Map[String, (FieldType, Set[AnyRef])], returnValue: V)