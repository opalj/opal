package org.opalj
package bugpicker
package core
package reporting

import scala.xml.Node
import scala.xml.Text
import org.opalj.bugpicker.core.analysis.Issue
import org.opalj.ai.domain.l1.IntegerRangeValues

object XMLReporter {
    def localVariablesToXHTML(issue: Issue): Option[Node] = {
        if (issue.pc.isEmpty || issue.code.isEmpty)
            return None;

        val pc = issue.pc.get
        val code = issue.code.get

        if (issue.localVariables.isEmpty) {
            return None;
        }
        val localVariableValues = issue.localVariables.get

        val localVariableDefinitions = code.localVariablesAt(pc)
        if (localVariableDefinitions.isEmpty)
            return None;

        val lvsAsXHTML =
            for ((index, theLV) ← localVariableDefinitions.toSeq.sortWith((a, b) ⇒ a._1 < b._1)) yield {
                val localValue = localVariableValues(index)
                val localValueAsXHTML =
                    if (localValue == null)
                        <span class="warning">unused</span>
                    else {

                        if ((theLV.fieldType eq org.opalj.br.BooleanType) &&
                            // SPECIAL HANDLING IF THE VALUE IS AN INTEGER RANGE VALUE
                            localValue.isInstanceOf[IntegerRangeValues#IntegerRange]) {
                            val range = localValue.asInstanceOf[IntegerRangeValues#IntegerRange]
                            if (range.lowerBound == 0 && range.upperBound == 0)
                                Text("false")
                            else if (range.lowerBound == 1 && range.upperBound == 1)
                                Text("true")
                            else
                                Text("true or false")
                        } else
                            Text(localValue.toString)
                    }

                <tr>
                    <td>{ index }</td><td>{ theLV.name }</td><td>{ localValueAsXHTML }</td>
                </tr>
            }

        Some(
            <details class="locals">
                <summary>Local Variable State [pc={ pc + issue.line(pc).map(" line="+_).getOrElse("") }]</summary>
                <table>
                    <tr><th>Index</th><th>Name</th><th>Value</th></tr>
                    { lvsAsXHTML }
                </table>
            </details>
        )

    }
}