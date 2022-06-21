/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import org.opalj.da.ClassFile
import org.opalj.br.MethodDescriptor
import org.opalj.br.MethodWithBody
import org.opalj.br.ObjectType
import org.opalj.br.analyses.MethodInfo
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.Instruction
import org.opalj.ai.BaseAI
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse

/**
 * Counts (non-)trivial usages of "Class.forName(...)".
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
class TrivialReflectionUsage(implicit hermes: HermesConfig) extends FeatureQuery {

    final val TrivialForNameUsage = "Trivial Class.forName Usage"
    final val NonTrivialForNameUsage = "Nontrivial Class.forName Usage"

    override val featureIDs: List[String] = List(TrivialForNameUsage, NonTrivialForNameUsage)

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(ClassFile, S)]
    ): IterableOnce[Feature[S]] = {
        val Class = ObjectType.Class
        val ForName1MD = MethodDescriptor("(Ljava/lang/String;)Ljava/lang/Class;")
        val ForName3MD =
            MethodDescriptor("(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;")

        val trivialLocations = new LocationsContainer[S]
        val nontrivialLocations = new LocationsContainer[S]

        project.parForeachMethodWithBody(isInterrupted = this.isInterrupted _) { mi =>
            val MethodInfo(source, m @ MethodWithBody(code)) = mi
            val classForNameCalls = code collect ({
                case i @ INVOKESTATIC(Class, false, "forName", ForName1MD | ForName3MD) => i
            }: PartialFunction[Instruction, INVOKESTATIC])
            if (classForNameCalls.nonEmpty) {
                val aiResult = BaseAI(m, new DefaultDomainWithCFGAndDefUse(project, m))
                val methodLocation = MethodLocation(source, m)
                for {
                    pcAndInstruction <- classForNameCalls
                    instruction = pcAndInstruction.value
                    pc = pcAndInstruction.pc
                    classNameParameterIndex = instruction.parametersCount - 1
                    operands = aiResult.operandsArray(pc) // if i is dead... opeands is null
                    if operands ne null
                    classNameParameter = operands(classNameParameterIndex)
                } {
                    classNameParameter match {
                        case aiResult.domain.StringValue(className) =>
                            trivialLocations += InstructionLocation(methodLocation, pc)
                        case aiResult.domain.MultipleReferenceValues(classNameParameters) =>
                            val classNames = classNameParameters.collect {
                                case aiResult.domain.StringValue(className) => className
                            }
                            // check if we have a concrete string in all cases..
                            val locations = if (classNames.size == classNameParameters.size) {
                                trivialLocations
                            } else {
                                nontrivialLocations
                            }
                            locations += InstructionLocation(methodLocation, pc)
                        case _ =>
                            nontrivialLocations += InstructionLocation(methodLocation, pc)
                    }
                }
            }
        }

        List(
            Feature[S](TrivialForNameUsage, trivialLocations),
            Feature[S](NonTrivialForNameUsage, nontrivialLocations)
        )
    }
}
