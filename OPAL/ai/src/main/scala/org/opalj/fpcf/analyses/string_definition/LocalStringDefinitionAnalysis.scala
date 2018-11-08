/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.FPCFAnalysis
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.analyses.string_definition.expr_processing.ExprHandler
import org.opalj.fpcf.string_definition.properties.StringTree
import org.opalj.fpcf.string_definition.properties.StringTreeCond
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.Stmt

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

class StringTrackingAnalysisContext(
    val stmts: Array[Stmt[V]]
)

/**
 * LocalStringDefinitionAnalysis processes a read operation of a local string variable at a program
 * position, ''pp'', in a way that it finds the set of possible strings that can be read at ''pp''.
 *
 * "Local" as this analysis takes into account only the enclosing function as a context. Values
 * coming from other functions are regarded as dynamic values even if the function returns a
 * constant string value. [[StringConstancyProperty]] models this by inserting "*" into the set of
 * possible strings.
 *
 * StringConstancyProperty might contain more than one possible string, e.g., if the source of the
 * value is an array.
 *
 * @author Patrick Mell
 */
class LocalStringDefinitionAnalysis(
        val project: SomeProject
) extends FPCFAnalysis {

    def analyze(data: P): PropertyComputationResult = {
        val tacProvider = p.get(SimpleTACAIKey)
        val stmts = tacProvider(data._2).stmts

        val exprHandler = ExprHandler(p, data._2)
        val defSites = data._1.definedBy.toArray.sorted
        if (ExprHandler.isStringBuilderToStringCall(stmts(defSites.head).asAssignment.expr)) {
            val subtrees = ArrayBuffer[StringTree]()
            defSites.foreach { nextDefSite ⇒
                val treeElements = ExprHandler.getDefSitesOfToStringReceiver(
                    stmts(nextDefSite).asAssignment.expr
                ).map { exprHandler.processDefSite }.filter(_.isDefined).map { _.get }
                if (treeElements.length == 1) {
                    subtrees.append(treeElements.head)
                } else {
                    subtrees.append(StringTreeCond(treeElements.to[ListBuffer]))
                }
            }

            val finalTree = if (subtrees.size == 1) subtrees.head else
                StringTreeCond(subtrees.to[ListBuffer])
            Result(data, StringConstancyProperty(finalTree))
        } // If not a call to StringBuilder.toString, then we deal with pure strings
        else {
            Result(data, StringConstancyProperty(
                exprHandler.processDefSites(data._1.definedBy.toArray).get
            ))
        }
    }

}

sealed trait LocalStringDefinitionAnalysisScheduler extends ComputationSpecification {

    final override def derives: Set[PropertyKind] = Set(StringConstancyProperty)

    final override def uses: Set[PropertyKind] = {
        Set()
    }

    final override type InitializationData = Null

    final def init(p: SomeProject, ps: PropertyStore): Null = null

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}

}

/**
 * Executor for the lazy analysis.
 */
object LazyStringDefinitionAnalysis
        extends LocalStringDefinitionAnalysisScheduler
        with FPCFLazyAnalysisScheduler {

    final override def startLazily(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): FPCFAnalysis = {
        val analysis = new LocalStringDefinitionAnalysis(p)
        ps.registerLazyPropertyComputation(StringConstancyProperty.key, analysis.analyze)
        analysis
    }

}
