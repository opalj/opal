/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import java.io.File
import java.io.FileInputStream

import scala.collection.mutable.ArrayBuffer

import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes

import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.FieldType
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.PCAndInstruction
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.br.fpcf.properties.SimpleContexts
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.br.instructions.Instruction

/**
 * Representation of all Methods that are reachable in the represented call graph.
 * TODO: This classes are copy&paste code from the JCG project and should be included as Dependency.
 *
 * @author Florian Kuebler
 */
case class ReachableMethodsDescription(reachableMethods: List[ReachableMethodDescription]) {

    /**
     * Converts the set of reachable methods into a mapping from method to the set of call sites.
     */
    lazy val toMap: Map[MethodDesc, List[CallSiteDescription]] = {
        reachableMethods.groupBy(_.method).map { case (k, v) => k -> v.flatMap(_.callSites) }
    }
}

object ReachableMethodsDescription {
    implicit val reachableMethodsReads: Reads[ReachableMethodsDescription] = Json.reads[ReachableMethodsDescription]

    implicit val reachableMethodsWrites: Writes[ReachableMethodsDescription] = Json.writes[ReachableMethodsDescription]
}

/**
 * A reachable method contains of the `method` itself and the call sites within that method.
 */
case class ReachableMethodDescription(method: MethodDesc, callSites: List[CallSiteDescription])

object ReachableMethodDescription {
    implicit val reachableMethodsReads: Reads[ReachableMethodDescription] = Json.reads[ReachableMethodDescription]

    implicit val reachableMethodsWrites: Writes[ReachableMethodDescription] = Json.writes[ReachableMethodDescription]
}

/**
 * A call site has a `declaredTarget` method, is associated with a line number (-1 if unknown) and
 * contains the set of computed target methods (`targets`).
 */
case class CallSiteDescription(
        declaredTarget: MethodDesc, line: Int, pc: Option[Int], targets: List[MethodDesc]
)

object CallSiteDescription {
    implicit val callSiteReads: Reads[CallSiteDescription] = Json.reads[CallSiteDescription]

    implicit val callSiteWrites: Writes[CallSiteDescription] = Json.writes[CallSiteDescription]
}

/**
 * A method is represented using the `name`, the `declaringClass`, its `returnType` and its
 * `parameterTypes`.
 */
case class MethodDesc(name: String, declaringClass: String, returnType: String, parameterTypes: List[String]) {

    override def toString: String = {
        s"$declaringClass { $returnType $name(${parameterTypes.mkString(", ")})}"
    }

    def nameBasedEquals(other: MethodDesc): Boolean = {
        other.name == this.name && other.declaringClass == this.declaringClass
    }

    def toDeclaredMethod(implicit declaredMethods: DeclaredMethods): DeclaredMethod = {
        val cfType = FieldType(declaringClass).asObjectType
        val desc = MethodDescriptor(s"(${parameterTypes.mkString("")})$returnType")
        declaredMethods(cfType, cfType.packageName, cfType, name, desc)
    }
}

object MethodDesc {
    implicit val methodReads: Reads[MethodDesc] = Json.reads[MethodDesc]

    implicit val methodWrites: Writes[MethodDesc] = Json.writes[MethodDesc]
}

/**
 * Reads the given serialized CG and stores the relations into the propertyStore.
 * The call graphs must be given in the JCG format.
 *
 * IMPROVE: Currently uses the Play's JSON API, which is not optimized for large files.
 *
 * @author Florian Kuebler
 */
private class CallGraphDeserializer private[analyses] (
        final val serializedCG: File,
        final val project:      SomeProject
) extends FPCFAnalysis {
    private implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    private val simpleContexts: SimpleContexts = project.get(SimpleContextsKey)

    private val data: Map[MethodDesc, List[CallSiteDescription]] = Json.parse(
        new FileInputStream(serializedCG)
    ).validate[ReachableMethodsDescription].get.toMap

    def analyze(p: SomeProject): PropertyComputationResult = {
        val results = ArrayBuffer.empty[ProperPropertyComputationResult]
        for (
            (methodDesc, callSites) <- data
        ) {
            val calls = new DirectCalls()
            val method = methodDesc.toDeclaredMethod
            for (
                x <- callSites.groupBy(cs => (cs.declaredTarget, cs.line)).values;
                (CallSiteDescription(declaredTgtDesc, line, pcOpt, tgts), index) <- x.zipWithIndex
            ) {

                val pc = if (pcOpt.isDefined)
                    pcOpt.get
                else
                    getPCFromLineNumber(method, line, declaredTgtDesc.toDeclaredMethod, index)

                val context = simpleContexts(method)

                for (tgtDesc <- tgts) {
                    calls.addCall(context, pc, simpleContexts(tgtDesc.toDeclaredMethod))
                }
                results ++= calls.partialResults(context)
            }
        }

        Results(results)
    }

    private[this] def getPCFromLineNumber(
        dm: DeclaredMethod, lineNumber: Int, declaredTgt: DeclaredMethod, index: Int
    ): Int = {
        if (!dm.hasSingleDefinedMethod)
            return 0;

        val method = dm.definedMethod
        val bodyOpt = method.body

        if (bodyOpt.isEmpty)
            return 0;

        val body = bodyOpt.get

        // FIXME this won't work when the code has no line numbers, resulting in all calls mapped to pc 0
        val pf = new PartialFunction[PCAndInstruction, Instruction] {
            override def isDefinedAt(pcAndInst: PCAndInstruction): Boolean = {
                val lnOpt = body.lineNumber(pcAndInst.pc)
                lnOpt.isDefined && {
                    lnOpt.get == lineNumber && {
                        val inst = pcAndInst.instruction
                        inst.isInvocationInstruction && {
                            val invokeInst = inst.asInvocationInstruction
                            invokeInst.name == declaredTgt.name &&
                                invokeInst.methodDescriptor == declaredTgt.descriptor
                        }
                    }
                }
            }

            override def apply(pcAndInst: PCAndInstruction) = pcAndInst.instruction
        }

        val instructions = body.collectInstructionsWithPC(pf)

        if (!instructions.isDefinedAt(index))
            return 0;

        instructions(index).pc
    }
}

class CallGraphDeserializerScheduler(serializedCG: File) extends BasicFPCFEagerAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, SimpleContextsKey)

    override def start(p: SomeProject, ps: PropertyStore, i: Null): FPCFAnalysis = {
        val analysis = new CallGraphDeserializer(serializedCG, p)
        ps.scheduleEagerComputationForEntity(p)(analysis.analyze)
        analysis
    }

    override def uses: Set[PropertyBounds] = Set.empty

    override def derivesEagerly: Set[PropertyBounds] = PropertyBounds.ubs(Callees, Callers)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty
}
