/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org
package opalj
package tac
package fpcf
package analyses
package string_analysis

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Property
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.Path
import org.opalj.tac.fpcf.properties.TACAI

/**
 * This class is to be used to store state information that are required at a later point in
 * time during the analysis, e.g., due to the fact that another analysis had to be triggered to
 * have all required information ready for a final result.
 */
trait ComputationState {
    val dm: DefinedMethod

    /**
     * The entity for which the analysis was started with.
     */
    val entity: SContext

    /**
     * The Three-Address Code of the entity's method
     */
    var tac: TAC = _

    /**
     * The computed lean path that corresponds to the given entity
     */
    var computedLeanPath: Path = _

    var tacDependee: Option[EOptionP[Method, TACAI]] = _

    val fpe2EPSDependees: mutable.Map[Int, List[(EOptionP[Entity, Property], SomeEPS => IPResult)]] = mutable.Map()
    val fpe2iprDependees: mutable.Map[Int, (List[IPResult], IPResult => IPResult)] = mutable.Map()

    /**
     * If not empty, this routine can only produce an intermediate result
     */
    var dependees: List[EOptionP[Entity, Property]] = List()

    /**
     * A mapping from DUVar elements to the corresponding values of the FlatPathElements
     */
    val var2IndexMapping: mutable.Map[SEntity, ListBuffer[Int]] = mutable.Map()

    /**
     * A mapping from values of FlatPathElements to an interpretation result represented by an [[IPResult]]
     */
    val fpe2ipr: mutable.Map[Int, IPResult] = mutable.Map()

    /**
     * An analysis may depend on the evaluation of its parameters. This number indicates how many
     * of such dependencies are still to be computed.
     */
    var parameterDependeesCount = 0

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
     * Takes an entity as well as a pc and append it to [[var2IndexMapping]].
     */
    def appendToVar2IndexMapping(entity: SEntity, pc: Int): Unit = {
        if (!var2IndexMapping.contains(entity)) {
            var2IndexMapping(entity) = ListBuffer()
        }
        var2IndexMapping(entity).append(pc)
    }
}
