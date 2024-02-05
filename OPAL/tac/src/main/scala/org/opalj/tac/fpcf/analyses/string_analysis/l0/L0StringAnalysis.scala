/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.FPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimLUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.l0.interpretation.L0InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.FlatPathElement
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.NestedPathElement
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.Path
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.PathTransformer
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.SubPath
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.WindowPathFinder
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.value.ValueInformation

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * IntraproceduralStringAnalysis processes a read operation of a local string variable at a program
 * position, ''pp'', in a way that it finds the set of possible strings that can be read at ''pp''.
 * <p>
 * This analysis takes into account only the enclosing function as a context, i.e., it is
 * intraprocedural. Values coming from other functions are regarded as dynamic values even if the
 * function returns a constant string value.
 * <p>
 * From a high-level perspective, this analysis works as follows. First, it has to be differentiated
 * whether string literals / variables or String{Buffer, Builder} are to be processed.
 * For the former, the definition sites are processed. Only one definition site is the trivial case
 * and directly corresponds to a leaf node in the string tree (such trees consist of only one node).
 * Multiple definition sites indicate > 1 possible initialization values and are transformed into a
 * string tree whose root node is an OR element and the children are the possible initialization
 * values. Note that all this is handled by [[StringConstancyInformation.reduceMultiple]].
 * <p>
 * For the latter, String{Buffer, Builder}, lean paths from the definition sites to the usage
 * (indicated by the given DUVar) is computed. That is, all paths from all definition sites to the
 * usage where only statements are contained that include the String{Builder, Buffer} object of
 * interest in some way (like an "append" or "replace" operation for example). These paths are then
 * transformed into a string tree by making use of a [[PathTransformer]].
 *
 * @author Patrick Mell
 */
class L0StringAnalysis(val project: SomeProject) extends FPCFAnalysis {

    /**
     * This class is to be used to store state information that are required at a later point in
     * time during the analysis, e.g., due to the fact that another analysis had to be triggered to
     * have all required information ready for a final result.
     */
    private case class ComputationState(
            // The lean path that was computed
            computedLeanPath: Path,
            // A mapping from DUVar elements to the corresponding indices of the FlatPathElements
            var2IndexMapping: mutable.Map[SEntity, Int],
            // A mapping from values of FlatPathElements to StringConstancyInformation
            fpe2sci: mutable.Map[Int, StringConstancyInformation],
            // The three-address code of the method in which the entity under analysis resides
            tac: TACode[TACMethodParameter, DUVar[ValueInformation]]
    )

    def analyze(data: SContext): ProperPropertyComputationResult = {
        // sci stores the final StringConstancyInformation (if it can be determined now at all)
        var sci = StringConstancyInformation.lb

        // Retrieve TAC from property store
        val tacOpt: Option[TACode[TACMethodParameter, V]] = ps(data._2, TACAI.key) match {
            case UBP(tac) => if (tac.tac.isEmpty) None else Some(tac.tac.get)
            case _        => None
        }
        // No TAC available, e.g., because the method has no body
        if (tacOpt.isEmpty)
            return Result(data, StringConstancyProperty.lb) // TODO add continuation

        implicit val tac: TACode[TACMethodParameter, V] = tacOpt.get
        val stmts = tac.stmts

        val puVar = data._1
        val uVar = puVar.toValueOriginForm(tac.pcToIndex)
        val defSites = uVar.definedBy.toArray.sorted
        // Function parameters are currently regarded as dynamic value; the following if finds read
        // operations of strings (not String{Builder, Buffer}s, they will be handled further down
        if (defSites.head < 0) {
            return Result(data, StringConstancyProperty.lb)
        }

        // If not empty, this very routine can only produce an intermediate result
        val dependees: mutable.Map[SContext, ListBuffer[EOptionP[SContext, StringConstancyProperty]]] = mutable.Map()
        // state will be set to a non-null value if this analysis needs to call other analyses /
        // itself; only in the case it calls itself, will state be used, thus, it is valid to
        // initialize it with null
        var state: ComputationState = null

        val call = stmts(defSites.head).asAssignment.expr
        if (InterpretationHandler.isStringBuilderBufferToStringCall(call)) {
            val initDefSites = InterpretationHandler.findDefSiteOfInit(uVar, stmts)
            if (initDefSites.isEmpty) {
                // String{Builder,Buffer} from method parameter is to be evaluated
                return Result(data, StringConstancyProperty.lb)
            }

            val path = new WindowPathFinder(tac.cfg).findPaths(initDefSites, uVar.definedBy.head)
            val leanPath = path.makeLeanPath(uVar, stmts)

            // Find DUVars, that the analysis of the current entity depends on
            val dependentVars = findDependentVars(leanPath, stmts, puVar)
            if (dependentVars.nonEmpty) {
                state = ComputationState(leanPath, dependentVars, mutable.Map[Int, StringConstancyInformation](), tac)
                dependentVars.keys.foreach { nextVar =>
                    propertyStore((nextVar, data._2), StringConstancyProperty.key) match {
                        case finalEP: FinalEP[SContext, StringConstancyProperty] =>
                            if (dependees.contains(data))
                                dependees(data) = dependees(data).filter { _.e != finalEP.e }
                            val sciOpt = processFinalP(dependees.values.flatten, state, finalEP)
                            if (sciOpt.isDefined)
                                sci = sciOpt.get
                        case ep =>
                            dependees.getOrElseUpdate(data, ListBuffer()).append(ep)
                    }
                }
            } else {
                val stringTree = new PathTransformer(L0InterpretationHandler(tac)).pathToStringTree(leanPath)
                sci = stringTree.reduce(true)
            }
        } else {
            // We deal with pure strings
            val interpretationHandler = L0InterpretationHandler(tac)
            sci = StringConstancyInformation.reduceMultiple(
                uVar.definedBy.toArray.sorted.map { ds =>
                    interpretationHandler.processDefSite(ds).p.stringConstancyInformation
                }
            )
        }

        if (dependees.nonEmpty) {
            InterimResult(
                data._1,
                StringConstancyProperty.ub,
                StringConstancyProperty.lb,
                dependees.values.flatten.toSet,
                continuation(data, dependees.values.flatten, state)
            )
        } else {
            Result(data, StringConstancyProperty(sci))
        }
    }

    private def processFinalP(
        dependees: Iterable[EOptionP[SContext, StringConstancyProperty]],
        state:     ComputationState,
        finalEP:   FinalEP[SContext, StringConstancyProperty]
    ): Option[StringConstancyInformation] = {
        // Add mapping information (which will be used for computing the final result)
        state.fpe2sci.put(state.var2IndexMapping(finalEP.e._1), finalEP.p.stringConstancyInformation)

        if (dependees.isEmpty) {
            val sci = new PathTransformer(L0InterpretationHandler(state.tac)).pathToStringTree(
                state.computedLeanPath,
                state.fpe2sci.map { case (k, v) => (k, ListBuffer(v)) }
            ).reduce(true)
            Some(sci)
        } else {
            None
        }
    }

    /**
     * Continuation function.
     *
     * @param data The data that was passed to the `analyze` function.
     * @param dependees A list of dependencies that this analysis run depends on.
     * @param state The computation state (which was originally captured by `analyze` and possibly
     *              extended / updated by other methods involved in computing the final result.
     * @return This function can either produce a final result or another intermediate result.
     */
    private def continuation(
        data:      SContext,
        dependees: Iterable[EOptionP[SContext, StringConstancyProperty]],
        state:     ComputationState
    )(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        case finalEP: FinalEP[_, _] =>
            val finalScpEP = finalEP.asInstanceOf[FinalEP[SContext, StringConstancyProperty]]
            val sciOpt = processFinalP(dependees, state, finalScpEP)
            if (sciOpt.isDefined) {
                val finalSci = new PathTransformer(L0InterpretationHandler(state.tac)).pathToStringTree(
                    state.computedLeanPath,
                    state.fpe2sci.map { case (k, v) => (k, ListBuffer(v)) }
                ).reduce(true)
                Result(data, StringConstancyProperty(finalSci))
            } else {
                val remainingDependees = dependees.filter { _.e != finalScpEP.e }
                InterimResult(
                    data,
                    StringConstancyProperty.ub,
                    StringConstancyProperty.lb,
                    remainingDependees.toSet,
                    continuation(data, remainingDependees, state)
                )
            }

        case InterimLUBP(lb, ub) => InterimResult(data, lb, ub, dependees.toSet, continuation(data, dependees, state))
        case _                   => throw new IllegalStateException("Could not process the continuation successfully.")
    }

    /**
     * Helper / accumulator function for finding dependees. For how dependees are detected, see
     * [[findDependentVars]]. Returns a list of pairs of DUVar and the index of the
     * [[FlatPathElement.element]] in which it occurs.
     */
    private def findDependeesAcc(
        subpath: SubPath,
        stmts:   Array[Stmt[V]]
    )(implicit tac: TACode[TACMethodParameter, V]): ListBuffer[(SEntity, Int)] = {
        val foundDependees = ListBuffer[(SEntity, Int)]()
        subpath match {
            case fpe: FlatPathElement =>
                // For FlatPathElements, search for DUVars on which the toString method is called
                // and where these toString calls are the parameter of an append call
                stmts(fpe.element) match {
                    case ExprStmt(_, outerExpr) =>
                        if (InterpretationHandler.isStringBuilderBufferAppendCall(outerExpr)) {
                            val param = outerExpr.asVirtualFunctionCall.params.head.asVar
                            param.definedBy.filter(_ >= 0).foreach { ds =>
                                val expr = stmts(ds).asAssignment.expr
                                // TODO check support for passing nested string builder directly (e.g. with a test case)
                                if (InterpretationHandler.isStringBuilderBufferToStringCall(expr)) {
                                    foundDependees.append((param.toPersistentForm(tac.stmts), fpe.element))
                                }
                            }
                        }
                    case _ =>
                }
                foundDependees
            case npe: NestedPathElement =>
                npe.element.foreach { nextSubpath =>
                    foundDependees.appendAll(findDependeesAcc(nextSubpath, stmts))
                }
                foundDependees
            case _ => foundDependees
        }
    }

    /**
     * Takes a `path`, this should be the lean path of a [[Path]], as well as a context in the form
     * of statements, `stmts`, and detects all dependees within `path`. Dependees are found by
     * looking at all elements in the path, and check whether the argument of an `append` call is a
     * value that stems from a `toString` call of a [[StringBuilder]] or [[StringBuffer]]. This
     * function then returns the found UVars along with the indices of those append statements.
     *
     * @note In order to make sure that a [[org.opalj.tac.DUVar]] does not depend on itself, pass
     *       this variable as `ignore`.
     */
    private def findDependentVars(
        path:   Path,
        stmts:  Array[Stmt[V]],
        ignore: SEntity
    )(implicit tac: TACode[TACMethodParameter, V]): mutable.LinkedHashMap[SEntity, Int] = {
        val dependees = mutable.LinkedHashMap[SEntity, Int]()

        path.elements.foreach { nextSubpath =>
            findDependeesAcc(nextSubpath, stmts).foreach { nextPair =>
                if (ignore != nextPair._1) {
                    dependees.put(nextPair._1, nextPair._2)
                }
            }
        }
        dependees
    }
}

sealed trait L0StringAnalysisScheduler extends FPCFAnalysisScheduler {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(StringConstancyProperty)

    override final def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(TACAI),
        PropertyBounds.ub(Callees),
        PropertyBounds.lub(StringConstancyProperty)
    )

    override final type InitializationData = L0StringAnalysis
    override final def init(p: SomeProject, ps: PropertyStore): InitializationData = {
        new L0StringAnalysis(p)
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}
}

object LazyL0StringAnalysis
    extends L0StringAnalysisScheduler with FPCFLazyAnalysisScheduler {

    override def register(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: InitializationData
    ): FPCFAnalysis = {
        val analysis = new L0StringAnalysis(p)
        ps.registerLazyPropertyComputation(StringConstancyProperty.key, analysis.analyze)
        analysis
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def requiredProjectInformation: ProjectInformationKeys = Seq(EagerDetachedTACAIKey)
}
