/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition

import org.opalj.br.analyses.SomeProject
import org.opalj.br.cfg.CFG
import org.opalj.fpcf.FPCFAnalysis
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.analyses.string_definition.preprocessing.AbstractPathFinder
import org.opalj.fpcf.analyses.string_definition.preprocessing.DefaultPathFinder
import org.opalj.fpcf.analyses.string_definition.preprocessing.PathTransformer
import org.opalj.fpcf.Result
import org.opalj.fpcf.analyses.string_definition.interpretation.InterpretationHandler
import org.opalj.fpcf.analyses.string_definition.preprocessing.FlatPathElement
import org.opalj.fpcf.analyses.string_definition.preprocessing.NestedPathElement
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.Stmt
import org.opalj.fpcf.analyses.string_definition.preprocessing.Path
import org.opalj.fpcf.analyses.string_definition.preprocessing.SubPath
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.IntermediateEP
import org.opalj.fpcf.IntermediateResult
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.ExprStmt
import org.opalj.tac.TACStmts

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

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

    /**
     * This class is to be used to store state information that are required at a later point in
     * time during the analysis, e.g., due to the fact that another analysis had to be triggered to
     * have all required information ready for a final result.
     */
    private case class ComputationState(
        // The lean path that was computed
        computedLeanPath: Path,
        // A mapping from DUVar elements to the corresponding indices of the FlatPathElements
        var2IndexMapping: mutable.LinkedHashMap[V, Int],
        // The control flow graph on which the computedLeanPath is based
        cfg: CFG[Stmt[V], TACStmts[V]]
    )

    /**
     * As executions of this analysis can be nested (since it may start itself), there might be
     * several states to capture. In order to do so and enable each analysis instance to access its
     * information, a map is used where the keys are the values fed into the analysis (which
     * uniquely identify an analysis run) and the values the corresponding states.
     */
    private[this] val states = mutable.Map[P, ComputationState]()

    def analyze(data: P): PropertyComputationResult = {
        // scis stores the final StringConstancyInformation
        val scis = ListBuffer[StringConstancyInformation]()
        val tacProvider = p.get(SimpleTACAIKey)
        val stmts = tacProvider(data._2).stmts
        val cfg = tacProvider(data._2).cfg

        // If not empty, this routine can only produce an intermediate result
        val dependees = mutable.Map[Entity, EOptionP[Entity, Property]]()

        data._1.foreach { nextUVar ⇒
            val defSites = nextUVar.definedBy.toArray.sorted
            val expr = stmts(defSites.head).asAssignment.expr
            val pathFinder: AbstractPathFinder = new DefaultPathFinder()
            if (InterpretationHandler.isStringBuilderBufferToStringCall(expr)) {
                val initDefSites = InterpretationHandler.findDefSiteOfInit(
                    expr.asVirtualFunctionCall, stmts
                )
                val paths = pathFinder.findPaths(initDefSites, cfg)
                val leanPaths = paths.makeLeanPath(nextUVar, stmts)

                // Find DUVars, that the analysis of the current entity depends on
                val dependentVars = findDependentVars(leanPaths, stmts, data._1)
                if (dependentVars.nonEmpty) {
                    val toAnalyze = (dependentVars.keys.toList, data._2)
                    val ep = propertyStore(toAnalyze, StringConstancyProperty.key)
                    ep match {
                        case FinalEP(_, p) ⇒
                            scis.appendAll(p.stringConstancyInformation)
                        case _ ⇒
                            dependees.put(toAnalyze, ep)
                            states.put(data, ComputationState(leanPaths, dependentVars, cfg))
                    }
                } else {
                    scis.append(new PathTransformer(cfg).pathToStringTree(leanPaths).reduce(true))
                }
            } // If not a call to String{Builder, Buffer}.toString, then we deal with pure strings
            else {
                val interHandler = InterpretationHandler(cfg)
                scis.append(StringConstancyInformation.reduceMultiple(
                    nextUVar.definedBy.toArray.sorted.flatMap { interHandler.processDefSite }.toList
                ))
            }
        }

        if (dependees.nonEmpty) {
            IntermediateResult(
                data,
                StringConstancyProperty.upperBound,
                StringConstancyProperty.lowerBound,
                dependees.values,
                continuation(data, dependees.values)
            )
        } else {
            Result(data, StringConstancyProperty(scis.toList))
        }
    }

    /**
     * Continuation function.
     *
     * @param data The data that was passed to the `analyze` function.
     * @param dependees A list of dependencies that this analysis run depends on.
     * @return This function can either produce a final result or another intermediate result.
     */
    private def continuation(
        data: P, dependees: Iterable[EOptionP[Entity, Property]]
    )(eps: SomeEPS): PropertyComputationResult = {
        val relevantState = states.get(data)
        // For mapping the index of a FlatPathElement to StringConstancyInformation
        val fpe2Sci = mutable.Map[Int, List[StringConstancyInformation]]()
        eps match {
            case FinalEP(e, p) ⇒
                val scis = p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
                // Add mapping information
                e.asInstanceOf[(List[V], _)]._1.asInstanceOf[List[V]].foreach { nextVar ⇒
                    fpe2Sci.put(relevantState.get.var2IndexMapping(nextVar), scis)
                }
                // Compute final result
                val sci = new PathTransformer(relevantState.get.cfg).pathToStringTree(
                    relevantState.get.computedLeanPath, fpe2Sci.toMap
                ).reduce(true)
                Result(data, StringConstancyProperty(List(sci)))
            case IntermediateEP(_, lb, ub) ⇒
                IntermediateResult(
                    data, lb, ub, dependees, continuation(data, dependees)
                )
            case _ ⇒ NoResult
        }

    }

    /**
     * Helper / accumulator function for finding dependees. For how dependees are detected, see
     * [[findDependentVars]]. Returns a list of pairs of DUVar and the index of the
     * [[FlatPathElement.element]] in which it occurs.
     */
    private def findDependeesAcc(
        subpath: SubPath, stmts: Array[Stmt[V]], foundDependees: ListBuffer[(V, Int)]
    ): ListBuffer[(V, Int)] = {
        subpath match {
            case fpe: FlatPathElement ⇒
                // For FlatPathElements, search for DUVars on which the toString method is called
                // and where these toString calls are the parameter of an append call
                stmts(fpe.element) match {
                    case ExprStmt(_, outerExpr) ⇒
                        if (InterpretationHandler.isStringBuilderBufferAppendCall(outerExpr)) {
                            val param = outerExpr.asVirtualFunctionCall.params.head.asVar
                            param.definedBy.foreach { ds ⇒
                                val expr = stmts(ds).asAssignment.expr
                                if (InterpretationHandler.isStringBuilderBufferToStringCall(expr)) {
                                    foundDependees.append((
                                        outerExpr.asVirtualFunctionCall.params.head.asVar,
                                        fpe.element
                                    ))
                                }
                            }
                        }
                    case _ ⇒
                }
                foundDependees
            case npe: NestedPathElement ⇒
                npe.element.foreach { nextSubpath ⇒
                    findDependeesAcc(nextSubpath, stmts, foundDependees)
                }
                foundDependees
            case _ ⇒ foundDependees
        }
    }

    /**
     * Takes a path, this should be the lean path of a [[Path]], as well as a context in the form of
     * statements, stmts, and detects all dependees within `path`. ''Dependees'' are found by
     * looking at all elements in the path, and check whether the argument of an `append` call is a
     * value that stems from a `toString` call of a [[StringBuilder]] or [[StringBuffer]]. This
     * function then returns the found UVars along with the indices of those append statements.
     *
     * @note In order to make sure that a [[org.opalj.tac.DUVar]] does not depend on itself, pass a
     *       `ignore` list (elements in `ignore` will not be added to the dependees list).
     */
    private def findDependentVars(
        path: Path, stmts: Array[Stmt[V]], ignore: List[V]
    ): mutable.LinkedHashMap[V, Int] = {
        val dependees = mutable.LinkedHashMap[V, Int]()
        path.elements.foreach { nextSubpath ⇒
            findDependeesAcc(nextSubpath, stmts, ListBuffer()).foreach { nextPair ⇒
                if (!ignore.contains(nextPair._1)) {
                    dependees.put(nextPair._1, nextPair._2)
                }
            }
        }
        dependees
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
