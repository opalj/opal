/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org
package opalj
package tac
package fpcf
package analyses
package string_analysis

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.br.DeclaredMethod
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Property
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.Path

/**
 * This class is to be used to store state information that are required at a later point in
 * time during the analysis, e.g., due to the fact that another analysis had to be triggered to
 * have all required information ready for a final result.
 */
trait ComputationState[State <: ComputationState[State]] {
    val dm: DeclaredMethod

    /**
     * The entity for which the analysis was started with.
     */
    val entity: SContext

    /**
     * The Three-Address Code of the entity's method
     */
    var tac: TAC = _ // TODO add dm to tac dependee mapping

    /**
     * The interpretation handler to use for computing a final result (if possible).
     */
    var iHandler: InterpretationHandler[State] = _

    /**
     * The interpretation handler to use for computing intermediate results. We need two handlers
     * since they have an internal state, e.g., processed def sites, which should not interfere
     * each other to produce correct results.
     */
    var interimIHandler: InterpretationHandler[State] = _

    /**
     * The computed lean path that corresponds to the given entity
     */
    var computedLeanPath: Path = _

    /**
     * If not empty, this routine can only produce an intermediate result
     */
    var dependees: List[EOptionP[Entity, Property]] = List()

    var dependeeDefSites: List[IPResult] = List()

    /**
     * A mapping from DUVar elements to the corresponding values of the FlatPathElements
     */
    val var2IndexMapping: mutable.Map[SEntity, ListBuffer[Int]] = mutable.Map()

    /**
     * A mapping from values of FlatPathElements to StringConstancyInformation
     */
    val fpe2sci: mutable.Map[Int, ListBuffer[StringConstancyInformation]] = mutable.Map()

    /**
     * A mapping from a value of a FlatPathElement to StringConstancyInformation which are not yet final.
     */
    val interimFpe2sci: mutable.Map[Int, ListBuffer[StringConstancyInformation]] = mutable.Map()

    /**
     * Used by [[appendToInterimFpe2Sci]] to track for which entities a value was appended to
     * [[interimFpe2sci]]. For a discussion of the necessity, see the documentation of
     * [[interimFpe2sci]].
     */
    private val entity2lastInterimFpe2SciValue: mutable.Map[SEntity, StringConstancyInformation] =
        mutable.Map()

    /**
     * An analysis may depend on the evaluation of its parameters. This number indicates how many
     * of such dependencies are still to be computed.
     */
    var parameterDependeesCount = 0

    /**
     * Indicates whether the basic setup of the string analysis is done. This value is to be set to
     * `true`, when all necessary dependees and parameters are available.
     */
    var isSetupCompleted = false

    /**
     * It might be that the result of parameters, which have to be evaluated, is not available right
     * away. Later on, when the result is available, it is necessary to map it to the right
     * position; this map stores this information. The key is the entity, with which the String
     * Analysis was started recursively; the value is a pair where the first value indicates the
     * index of the method and the second value the position of the parameter.
     */
    val paramResultPositions: mutable.Map[SContext, (Int, Int)] = mutable.Map()

    /**
     * Parameter values of a method / function. The structure of this field is as follows: Each item
     * in the outer list holds the parameters of a concrete call. A mapping from the definition
     * sites of parameter (negative values) to a correct index of `params` has to be made!
     */
    var params: ListBuffer[ListBuffer[StringConstancyInformation]] = ListBuffer()

    /**
     * This map is used to store information regarding arguments of function calls. In case a
     * function is passed as a function parameter, the result might not be available right away but
     * needs to be mapped to the correct param element of [[nonFinalFunctionArgs]] when available.
     * For this, this map is used.
     * For further information, see [[NonFinalFunctionArgsPos]].
     */
    val nonFinalFunctionArgsPos: NonFinalFunctionArgsPos = mutable.Map()

    /**
     * This map is used to actually store the interpretations of parameters passed to functions.
     * For further information, see [[NonFinalFunctionArgs]].
     */
    val nonFinalFunctionArgs: mutable.Map[FunctionCall[V], NonFinalFunctionArgs] = mutable.Map()

    /**
     * During the process of updating the [[nonFinalFunctionArgs]] map, it is necessary to find out
     * to which function an entity belongs. We use the following map to do this in constant time.
     */
    val entity2Function: mutable.Map[SContext, ListBuffer[FunctionCall[V]]] = mutable.Map()

    /**
     * Takes a `pc` as well as [[StringConstancyInformation]] and extends the [[fpe2sci]] map accordingly, however, only
     * if `pc` is not yet present or `sci` not present within the list of `pc`.
     */
    def appendToFpe2Sci(
        pc:    Int,
        sci:   StringConstancyInformation,
        reset: Boolean = false
    ): Unit = {
        if (reset || !fpe2sci.contains(pc)) {
            fpe2sci(pc) = ListBuffer()
        }
        if (!fpe2sci(pc).contains(sci)) {
            fpe2sci(pc).append(sci)
        }
    }

    /**
     * Appends a [[StringConstancyInformation]] element to [[interimFpe2sci]]. The rules for
     * appending are as follows:
     * <ul>
     *     <li>If no element has been added to the interim result list belonging to `defSite`, the
     *     element is guaranteed to be added.</li>
     *     <li>If no entity is given, i.e., `None`, and the list at `defSite` does not contain
     *     `sci`, `sci` is guaranteed to be added. If necessary, the oldest element in the list
     *     belonging to `defSite` is removed.</li>
     *     <li>If a non-empty entity is given, it is checked whether an entry for that element has
     *     been added before by making use of [[entity2lastInterimFpe2SciValue]]. If so, the list is
     *     updated only if that element equals [[StringConstancyInformation.lb]]. The reason being
     *     is that otherwise the result of updating the upper bound might always produce a new
     *     result which would not make the analysis terminate. Basically, it might happen that the
     *     analysis produces for an entity ''e_1'' the result "(e1|e2)" which the analysis of
     *     entity ''e_2'' uses to update its state to "((e1|e2)|e3)". The analysis of ''e_1'', which
     *     depends on ''e_2'' and vice versa, will update its state producing "((e1|e2)|e3)" which
     *     makes the analysis of ''e_2'' update its to (((e1|e2)|e3)|e3) and so on.</li>
     * </ul>
     *
     * @param pc The definition site to which append the given `sci` element for.
     * @param sci The [[StringConstancyInformation]] to add to the list of interim results for the
     *            given definition site.
     * @param entity Optional. The entity for which the `sci` element was computed.
     */
    def appendToInterimFpe2Sci(
        pc:     Int,
        sci:    StringConstancyInformation,
        entity: Option[SEntity] = None
    ): Unit = {
        val numElements = var2IndexMapping.values.flatten.count(_ == pc)
        var addedNewList = false
        if (!interimFpe2sci.contains(pc)) {
            interimFpe2sci(pc) = ListBuffer()
            addedNewList = true
        }
        // Append an element
        val containsSci = interimFpe2sci(pc).contains(sci)
        if (!containsSci && entity.isEmpty) {
            if (!addedNewList && interimFpe2sci(pc).length == numElements) {
                interimFpe2sci(pc).remove(0)
            }
            interimFpe2sci(pc).append(sci)
        } else if (!containsSci && entity.nonEmpty) {
            if (!entity2lastInterimFpe2SciValue.contains(entity.get) ||
                entity2lastInterimFpe2SciValue(entity.get) == StringConstancyInformation.lb
            ) {
                entity2lastInterimFpe2SciValue(entity.get) = sci
                if (interimFpe2sci(pc).nonEmpty) {
                    interimFpe2sci(pc).remove(0)
                }
                interimFpe2sci(pc).append(sci)
            }
        }
    }

    /**
     * Takes an entity as well as a pc and append it to [[var2IndexMapping]].
     */
    def appendToVar2IndexMapping(entity: SEntity, pc: Int): Unit = {
        if (!var2IndexMapping.contains(entity)) {
            var2IndexMapping(entity) = ListBuffer()
        }
        var2IndexMapping(entity).append(pc)
    }
}
