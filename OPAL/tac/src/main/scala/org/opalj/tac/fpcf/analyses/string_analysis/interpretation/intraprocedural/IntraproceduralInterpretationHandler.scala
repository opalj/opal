/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.intraprocedural

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.Property
import org.opalj.value.ValueInformation
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.tac.ArrayLoad
import org.opalj.tac.Assignment
import org.opalj.tac.BinaryExpr
import org.opalj.tac.ExprStmt
import org.opalj.tac.GetField
import org.opalj.tac.IntConst
import org.opalj.tac.New
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.StringConst
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.DoubleConst
import org.opalj.tac.FloatConst
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.common.BinaryExprInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.common.DoubleValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.common.FloatValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.common.IntegerValueInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.common.NewInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.common.StringConstInterpreter
import org.opalj.tac.DUVar
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode

/**
 * `IntraproceduralInterpretationHandler` is responsible for processing expressions that are
 * relevant in order to determine which value(s) a string read operation might have. These
 * expressions usually come from the definitions sites of the variable of interest.
 * <p>
 * For this interpretation handler it is crucial that all used interpreters (concrete instances of
 * [[org.opalj.tac.fpcf.analyses.string_analysis.interpretation.AbstractStringInterpreter]]) return
 * a final computation result!
 *
 * @author Patrick Mell
 */
class IntraproceduralInterpretationHandler(
        tac: TACode[TACMethodParameter, DUVar[ValueInformation]]
) extends InterpretationHandler(tac) {

    /**
     * Processed the given definition site in an intraprocedural fashion.
     * <p>
     * @inheritdoc
     */
    override def processDefSite(
        defSite: Int, params: List[Seq[StringConstancyInformation]] = List()
    ): EOptionP[Entity, Property] = {
        // Without doing the following conversion, the following compile error will occur: "the
        // result type of an implicit conversion must be more specific than org.opalj.fpcf.Entity"
        val e: Integer = defSite.toInt
        // Function parameters are not evaluated but regarded as unknown
        if (defSite < 0) {
            return FinalEP(e, StringConstancyProperty.lb)
        } else if (processedDefSites.contains(defSite)) {
            return FinalEP(e, StringConstancyProperty.getNeutralElement)
        }
        processedDefSites(defSite) = Unit

        val result: EOptionP[Entity, Property] = stmts(defSite) match {
            case Assignment(_, _, expr: StringConst) ⇒
                new StringConstInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: IntConst) ⇒
                new IntegerValueInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: FloatConst) ⇒
                new FloatValueInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: DoubleConst) ⇒
                new DoubleValueInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: ArrayLoad[V]) ⇒
                new IntraproceduralArrayInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: New) ⇒
                new NewInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: VirtualFunctionCall[V]) ⇒
                new IntraproceduralVirtualFunctionCallInterpreter(
                    cfg, this
                ).interpret(expr, defSite)
            case Assignment(_, _, expr: StaticFunctionCall[V]) ⇒
                new IntraproceduralStaticFunctionCallInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: BinaryExpr[V]) ⇒
                new BinaryExprInterpreter(cfg, this).interpret(expr, defSite)
            case Assignment(_, _, expr: NonVirtualFunctionCall[V]) ⇒
                new IntraproceduralNonVirtualFunctionCallInterpreter(
                    cfg, this
                ).interpret(expr, defSite)
            case Assignment(_, _, expr: GetField[V]) ⇒
                new IntraproceduralFieldInterpreter(cfg, this).interpret(expr, defSite)
            case ExprStmt(_, expr: VirtualFunctionCall[V]) ⇒
                new IntraproceduralVirtualFunctionCallInterpreter(
                    cfg, this
                ).interpret(expr, defSite)
            case ExprStmt(_, expr: StaticFunctionCall[V]) ⇒
                new IntraproceduralStaticFunctionCallInterpreter(cfg, this).interpret(expr, defSite)
            case vmc: VirtualMethodCall[V] ⇒
                new IntraproceduralVirtualMethodCallInterpreter(cfg, this).interpret(vmc, defSite)
            case nvmc: NonVirtualMethodCall[V] ⇒
                new IntraproceduralNonVirtualMethodCallInterpreter(
                    cfg, this
                ).interpret(nvmc, defSite)
            case _ ⇒ FinalEP(e, StringConstancyProperty.getNeutralElement)
        }
        result
    }

}

object IntraproceduralInterpretationHandler {

    /**
     * @see [[IntraproceduralInterpretationHandler]]
     */
    def apply(
        tac: TACode[TACMethodParameter, DUVar[ValueInformation]]
    ): IntraproceduralInterpretationHandler = new IntraproceduralInterpretationHandler(tac)

}
