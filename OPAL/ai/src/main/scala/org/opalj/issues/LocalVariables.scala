/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package issues

import scala.xml.Node
import scala.xml.Text
import scala.xml.Comment

import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.JsNull

import org.opalj.collection.mutable.Locals
import org.opalj.br.PC
import org.opalj.br.Code

/**
 * @param   localVariables The register values at the given location.
 */
class LocalVariables(
        val code:           Code,
        val pc:             PC,
        val localVariables: Locals[_ <: AnyRef]
) extends IssueDetails {

    def toXHTML(basicInfoOnly: Boolean): Node = {
        val localVariableDefinitions = code.localVariablesAt(pc)
        if (localVariableDefinitions.isEmpty)
            return Comment("local variable information are not found in the class file");

        if (basicInfoOnly)
            return Text("");

        val sortedLVDefs = localVariableDefinitions.toSeq.sortWith((a, b) => a._1 < b._1)
        val lvsAsXHTML =
            for ((index, theLV) <- sortedLVDefs) yield {
                val localValue = localVariables(index)
                val localValueAsXHTML =
                    if (localValue == null)
                        <span class="warning">unused</span>
                    else
                        Text(localVariableToString(theLV, localValue))

                <tr>
                    <td>{ index }</td><td>{ theLV.name }</td><td>{ localValueAsXHTML }</td>
                </tr>
            }

        <details class="locals">
            <summary>Local Variable State</summary>
            <table>
                <tr><th>Index</th><th>Name</th><th>Value</th></tr>
                { lvsAsXHTML }
            </table>
        </details>

    }

    def toAnsiColoredString: String = "" // TODO Support a better representation

    def toEclipseConsoleString: String = "" // TODO Support a better representation

    override def toIDL: JsValue = {
        val lvDefs = code.localVariablesAt(pc)
        val lvValues = lvDefs.toSeq.sortWith((a, b) => a._1 < b._1).map { e =>
            val (index, localVariable) = e
            val localValue = localVariables(index)
            Json.obj(
                "name" -> localVariable.name,
                "value" -> {
                    if (localValue == null)
                        JsNull
                    else
                        localVariableToString(localVariable, localValue)
                }
            )
        }
        Json.obj("type" -> "LocalVariables", "values" -> lvValues)
    }
}
