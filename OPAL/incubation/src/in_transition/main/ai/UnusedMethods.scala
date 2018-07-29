/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import java.net.URL
import org.opalj.br.ObjectType
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.ai.analyses.cg.{CallGraphFactory, VTACallGraphKey, ComputedCallGraph}
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.DefaultOneStepAnalysis

/**
 * A shallow analysis that tries to identify ((package) private) methods that are dead.
 *
 * @author Michael Eichberg
 */
object UnusedMethods extends DefaultOneStepAnalysis {

    override def title: String = "Dead methods"

    override def description: String = "Identifies methods that are never called."

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        implicit val classHierarchy = theProject.classHierarchy
        import classHierarchy.isSubtypeOf

        val results = {
            val ComputedCallGraph(callGraph, _, _) = theProject.get(VTACallGraphKey)
            for {
                classFile ← theProject.allProjectClassFiles.par
                if !isInterrupted()
                method ← classFile.methods
                if !method.isSynthetic
                if method.body.isDefined
                if method.isPrivate || method.hasDefaultVisibility
                if callGraph.calledBy(method).isEmpty
                if !(
                    (method.name == "<clinit>" || method.name == "<init>") &&
                    method.descriptor == MethodDescriptor.NoArgsAndReturnVoid
                )
                if !(
                    CallGraphFactory.isPotentiallySerializationRelated(method) &&
                    isSubtypeOf(classFile.thisType, ObjectType.Serializable).isYesOrUnknown
                )
            } yield {
                (classFile, method)
            }
        }
        val sortedResults =
            (
                results.seq.toSeq.sortWith { (e1, e2) ⇒
                    val (e1ClassFile, e1Method) = e1
                    val (e2ClassFile, e2Method) = e2
                    val e1FQN = e1ClassFile.thisType.fqn
                    val e2FQN = e2ClassFile.thisType.fqn
                    e1FQN < e2FQN || (e1FQN == e2FQN && e1Method < e2Method)
                }
            ).map(e ⇒ e._2.fullyQualifiedSignature)

        val msg = sortedResults.mkString("Dead Methods: "+results.size+"): \n", "\n", "\n")
        BasicReport(msg)
    }

}
