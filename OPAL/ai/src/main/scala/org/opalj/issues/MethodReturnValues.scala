/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package issues

import scala.xml.Node
import scala.xml.Group
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import org.opalj.br.Method
import org.opalj.br.ClassFile
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.ai.AIResult
import org.opalj.br.PCAndAnyRef

class MethodReturnValues(
        val method: Method,
        val result: AIResult
) extends IssueDetails with MethodComprehension {

    final def classFile: ClassFile = method.classFile

    private[this] implicit def code = result.code

    private[this] def operandsArray = result.operandsArray

    def collectMethodReturnValues: List[PCAndAnyRef[String]] = {
        code.foldLeft(List.empty[PCAndAnyRef[String]]) { (returnValues, pc, instruction) =>
            instruction match {
                case instr @ MethodInvocationInstruction(declaringClassType, _, name, descriptor) if !descriptor.returnType.isVoidType && {
                    val nextPC = instr.indexOfNextInstruction(pc)
                    val operands = operandsArray(nextPC)
                    operands != null &&
                        operands.head.isMorePreciseThan(result.domain.TypedValue(pc, descriptor.returnType))
                } =>
                    val modifier = if (instr.isInstanceOf[INVOKESTATIC]) "static " else ""
                    val nextPCOperandHead = operandsArray(instr.indexOfNextInstruction(pc)).head

                    PCAndAnyRef(
                        pc,
                        s"$nextPCOperandHead <- ${declaringClassType.toJava}{ $modifier ${descriptor.toJava(name)} }"
                    ) :: returnValues

                case _ => returnValues
            }

        }
    }

    def toXHTML(basicInfoOnly: Boolean): Node = {
        import PCLineComprehension.{pcNode, lineNode, line}
        val methodReturnValues =
            collectMethodReturnValues.map { methodData =>
                val pc = methodData.pc
                val details = methodData.value
                <li>
                    { pcNode(classFileFQN, methodJVMSignature, pc) }
                    &nbsp;
                    { lineNode(classFileFQN, methodJVMSignature, pc, line(pc)) }
                    <span class="value">{ details }</span>
                </li>
            }

        if (methodReturnValues.nonEmpty)
            <details class="method_return_values">
                <summary>Method Return Values</summary>
                <ul>{ methodReturnValues }</ul>
            </details>
        else
            Group(Nil)
    }

    def toAnsiColoredString: String = "" // TODO Support a better representation

    def toEclipseConsoleString: String = "" // TODO Support a better representation

    override def toIDL: JsValue = {
        import PCLineComprehension.line

        Json.obj(
            "type" -> "MethodReturnValues",
            "values" -> collectMethodReturnValues.map { methodData =>
                val pc = methodData.pc
                val details = methodData.value

                Json.obj(
                    "classFileFQN" -> classFileFQN,
                    "methodJVMSignature" -> methodJVMSignature,
                    "pc" -> pc,
                    "line" -> line(pc),
                    "details" -> details
                )
            }
        )
    }
}
