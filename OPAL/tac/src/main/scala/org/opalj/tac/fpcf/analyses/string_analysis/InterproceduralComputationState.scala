/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Property
import org.opalj.fpcf.Result
import org.opalj.value.ValueInformation
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.tac.Stmt
import org.opalj.tac.fpcf.analyses.string_analysis.preprocessing.Path
import org.opalj.tac.DUVar
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural.InterproceduralInterpretationHandler

/**
 * This class is to be used to store state information that are required at a later point in
 * time during the analysis, e.g., due to the fact that another analysis had to be triggered to
 * have all required information ready for a final result.
 *
 * @param entity The entity for which the analysis was started with.
 */
case class InterproceduralComputationState(entity: P) {
    // The Three-Address Code of the entity's method
    var tac: TACode[TACMethodParameter, DUVar[ValueInformation]] = _
    // The Control Flow Graph of the entity's method
    var cfg: CFG[Stmt[V], TACStmts[V]] = _
    // The interpretation handler to use
    var iHandler: InterproceduralInterpretationHandler = _
    // The computed lean path that corresponds to the given entity
    var computedLeanPath: Path = _
    // Callees information regarding the declared method that corresponds to the entity's method
    var callees: Callees = _
    // Callers information regarding the declared method that corresponds to the entity's method
    var callers: CallersProperty = _
    // If not empty, this routine can only produce an intermediate result
    var dependees: List[EOptionP[Entity, Property]] = List()
    // A mapping from DUVar elements to the corresponding indices of the FlatPathElements
    val var2IndexMapping: mutable.Map[V, Int] = mutable.Map()
    // A mapping from values / indices of FlatPathElements to StringConstancyInformation
    val fpe2sci: mutable.Map[Int, ListBuffer[StringConstancyInformation]] = mutable.Map()
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
    val paramResultPositions: mutable.Map[P, (Int, Int)] = mutable.Map()
    // Parameter values of a method / function. The structure of this field is as follows: Each item
    // in the outer list holds the parameters of a concrete call. A mapping from the definition
    // sites of parameter (negative values) to a correct index of `params` has to be made!
    var params: ListBuffer[ListBuffer[StringConstancyInformation]] = ListBuffer()

    /**
     * Takes a definition site as well as a result and extends the [[fpe2sci]] map accordingly,
     * however, only if `defSite` is not yet present.
     */
    def appendResultToFpe2Sci(
        defSite: Int, r: Result, reset: Boolean = false
    ): Unit = appendToFpe2Sci(
        defSite,
        StringConstancyProperty.extractFromPPCR(r).stringConstancyInformation,
        reset
    )

    /**
     * Takes a definition site as well as [[StringConstancyInformation]] and extends the [[fpe2sci]]
     * map accordingly, however, only if `defSite` is not yet present.
     */
    def appendToFpe2Sci(
        defSite: Int, sci: StringConstancyInformation, reset: Boolean = false
    ): Unit = {
        if (reset || !fpe2sci.contains(defSite)) {
            fpe2sci(defSite) = ListBuffer()
        }
        fpe2sci(defSite).append(sci)
    }

}
