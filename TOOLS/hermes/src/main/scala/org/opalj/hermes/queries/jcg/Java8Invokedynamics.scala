/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.hermes.queries.jcg

import scala.collection.immutable.ArraySeq

import org.opalj.da
import org.opalj.hermes.DefaultFeatureQuery
import org.opalj.hermes.HermesConfig
import org.opalj.hermes.InstructionLocation
import org.opalj.hermes.LocationsContainer
import org.opalj.hermes.ProjectConfiguration

import org.opalj.bi.REF_invokeInterface
import org.opalj.bi.REF_invokeSpecial
import org.opalj.bi.REF_invokeStatic
import org.opalj.bi.REF_invokeVirtual
import org.opalj.bi.REF_newInvokeSpecial
import org.opalj.br.InvokeSpecialMethodHandle
import org.opalj.br.InvokeStaticMethodHandle
import org.opalj.br.Method
import org.opalj.br.MethodCallMethodHandle
import org.opalj.br.MethodDescriptor
import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.AASTORE
import org.opalj.br.instructions.ARETURN
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.Instruction
import org.opalj.br.ObjectType.LambdaMetafactory
import org.opalj.ai.Domain
import org.opalj.ai.InterruptableAI
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse

/**
 * This feature query corresponds to the Java8Invokedynamics.md test cases from the JCG call
 * graph test suite as well as some infrastructure incompatabile tests pertaining to JVM-hosted
 * languages that also use invokedynamics or Java 10 invokedynamics that are used to concatenate
 * Strings.
 *
 * @author Michael Reif
 */
class Java8Invokedynamics(
        implicit
        val hermesConfig: HermesConfig
) extends DefaultFeatureQuery {

    override def featureIDs: Seq[String] =
        /* There are 16 test cases, 7 pertaining to method references and 9 pertaining to lambdas
        * Lambda5 = Java10 string concat
        * Lambda6 = Scala serialization
        * Lambda7 = Scala symbols
        * Lambda8 = Scala structural call site
        * Lambda9 = Groovy invDyn - no test case available
        *
        * Note: Scala's method references use the same mechanism as Java 8 and are therefore not
        * covered within this query.
        * */
        (1 to 7).map(num => s"MR$num") ++ (1 to 9).map(num => s"Lambda$num")

    def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(da.ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {
        import org.opalj.br.reader.InvokedynamicRewriting._

        val locations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        for {
            m @ MethodWithBody(code) <- project.allMethodsWithBody
            pcAndInvocation <- code collect ({
                case dynInv: INVOKEDYNAMIC => dynInv
            }: PartialFunction[Instruction, Instruction])
        } {

            val pc = pcAndInvocation.pc
            val l = InstructionLocation(project.source(m.classFile).get, m, pc)

            val testCaseId = pcAndInvocation.value match {
                case invDyn: INVOKEDYNAMIC =>
                    {

                        if (isJava10StringConcatInvokedynamic(invDyn)) {
                            11 /* Lambda5 */
                        } else if (isScalaLambdaDeserializeExpression(invDyn)) {
                            12 /* Lambda6 */
                        } else if (isScalaSymbolExpression(invDyn)) {
                            13 /* Lambda7 */
                        } else if (m.instructionsOption.nonEmpty
                            && isScalaStructuralCallSite(invDyn, m.instructionsOption.get, pc)) {
                            14 /* Lambda8 */
                        } else if (isGroovyInvokedynamic(invDyn)) {
                            15 /* Lambda9 */
                        } else if (isJava8LikeLambdaExpression(invDyn)) {
                            val bm = invDyn.bootstrapMethod
                            val handle = bm.arguments(1).asInstanceOf[MethodCallMethodHandle]

                            if (bm.handle.isInvokeStaticMethodHandle) {
                                val InvokeStaticMethodHandle(LambdaMetafactory, false, name, descriptor) = bm.handle
                                if (descriptor == MethodDescriptor.LambdaAltMetafactoryDescriptor &&
                                    name == "altMetafactory") {
                                    10 /*Lambda4 */
                                } else if (code.pcOfNextInstruction(pc) != -1) {
                                    val nextPC = code.pcOfNextInstruction(pc)
                                    if (code.instructions(nextPC).opcode == ARETURN.opcode)
                                        8 /* Lambda2*/
                                    else {
                                        val ai = new InterruptableAI[Domain]
                                        val domain = new DefaultDomainWithCFGAndDefUse(project, m)
                                        val result = ai(m, domain)
                                        val instructions = result.domain.code.instructions
                                        val users = result.domain.usedBy(pc)
                                        if (users.exists(instructions(_).opcode == AASTORE.opcode))
                                            9 /* Lambda3 */
                                        else
                                            handleJava8InvokeDynamic(m, handle)
                                    }
                                } else
                                    handleJava8InvokeDynamic(m, handle)
                            } else
                                handleJava8InvokeDynamic(m, handle)
                        } else {
                            //throw new RuntimeException("Unexpected handle Kind." + invDyn)
                            -1
                        }
                    }
            }

            if (testCaseId >= 0 && testCaseId < featureIDs.size) {
                locations(testCaseId) += l
            }
        }

        ArraySeq.unsafeWrapArray(locations)
    }

    private def handleJava8InvokeDynamic[S](m: Method, handle: MethodCallMethodHandle) = {
        handle.referenceKind match {
            case REF_invokeInterface => 0 /* MR1*/
            case REF_invokeStatic => {
                val InvokeStaticMethodHandle(_, _, name, descriptor) = handle
                // this just the called method is defined in the same class..
                // if there is a method in the same class with the same name and descriptor,
                // this check is tricked.
                val localMethod = m.classFile.findMethod(name, descriptor)
                val isLocal = localMethod.isDefined
                if (isLocal) {
                    val callee = localMethod.get
                    if (callee.isStatic) {
                        if (callee.isSynthetic) {
                            7 /* Lambda1 */
                        } else {
                            if (callee.parameterTypes.isEmpty) {
                                3 /* MR4 */
                            } else {
                                4 /* MR5 */
                            }
                        }
                    } else {
                        /* something unexpected */ -1
                    }
                } else {
                    8 /* Lambda2 */
                }
            }
            case REF_invokeSpecial => {
                val InvokeSpecialMethodHandle(_, isInterface, name, methodDescriptor) = handle
                val localMethod = m.classFile.findMethod(name, methodDescriptor)
                val isLocal = localMethod.isDefined
                if (isLocal) {
                    val callee = localMethod.get
                    if (callee.isSynthetic) 2 /* MR3  */ else 1 /* MR2 */
                } else /* something unexpected */ 10
            }
            case REF_invokeVirtual    => 6 /* MR 7 */
            case REF_newInvokeSpecial => 5 /* MR 6 */
            case hk                   => throw new RuntimeException("Unexpected handle Kind."+hk)
        }
    }
}