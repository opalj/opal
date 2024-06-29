/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package interpretation

import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * Processes expressions that are relevant in order to determine which value(s) the string value at a given def site
 * might have.
 *
 * [[InterpretationHandler]]s of any level may use [[StringInterpreter]]s from their level or any level below.
 * [[StringInterpreter]]s defined in the [[interpretation]] package may be used by any level.
 *
 * @author Maximilian Rüsch
 */
abstract class InterpretationHandler extends FPCFAnalysis {

    def analyze(entity: MethodPC): ProperPropertyComputationResult = {
        val tacaiEOptP = ps(entity.dm.definedMethod, TACAI.key)
        implicit val state: InterpretationState = InterpretationState(entity.pc, entity.dm, tacaiEOptP)

        if (tacaiEOptP.isRefinable) {
            InterimResult.forUB(
                InterpretationHandler.getEntity,
                StringFlowFunctionProperty.ub,
                Set(state.tacDependee),
                continuation(state)
            )
        } else if (tacaiEOptP.ub.tac.isEmpty) {
            // No TAC available, e.g., because the method has no body
            StringInterpreter.computeFinalResult(StringFlowFunctionProperty.constForAll(StringTreeNode.lb))
        } else {
            processStatementForState
        }
    }

    private def continuation(state: InterpretationState)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case FinalEP(_, _) if eps.pk.equals(TACAI.key) =>
                state.tacDependee = eps.asInstanceOf[FinalEP[Method, TACAI]]
                processStatementForState(state)

            case _ =>
                InterimResult.forUB(
                    InterpretationHandler.getEntity(state),
                    StringFlowFunctionProperty.ub,
                    Set(state.tacDependee),
                    continuation(state)
                )
        }
    }

    private def processStatementForState(implicit state: InterpretationState): ProperPropertyComputationResult = {
        val defSiteOpt = valueOriginOfPC(state.pc, state.tac.pcToIndex);
        if (defSiteOpt.isEmpty) {
            throw new IllegalArgumentException(s"Obtained a pc that does not represent a definition site: ${state.pc}")
        }

        processStatement(state)(state.tac.stmts(defSiteOpt.get))
    }

    protected def processStatement(
        implicit state: InterpretationState
    ): PartialFunction[Stmt[V], ProperPropertyComputationResult]
}

object InterpretationHandler {

    def getEntity(implicit state: InterpretationState): MethodPC = MethodPC(state.pc, state.dm)

    /**
     * This function checks whether a given type is a supported primitive type. Supported currently
     * means short, int, float, or double.
     */
    private def isSupportedPrimitiveNumberType(typeName: String): Boolean =
        typeName == "short" || typeName == "int" || typeName == "float" || typeName == "double"

    /**
     * Checks whether a given type, identified by its string representation, is supported by the
     * string analysis. That means, if this function returns `true`, a value, which is of type
     * `typeName` may be approximated by the string analysis better than just the lower bound.
     *
     * @param typeName The name of the type to check. May either be the name of a primitive type or
     *                 a fully-qualified class name (dot-separated).
     * @return Returns `true`, if `typeName` is an element in [char, short, int, float, double,
     *         java.lang.String] and `false` otherwise.
     */
    def isSupportedType(typeName: String): Boolean =
        typeName == "char" || isSupportedPrimitiveNumberType(typeName) ||
            typeName == "java.lang.String" || typeName == "java.lang.String[]"

    /**
     * Determines whether a given element is supported by the string analysis.
     *
     * @return Returns true if the given [[FieldType]] is of a supported type. For supported types,
     *         see [[InterpretationHandler.isSupportedType(String)]].
     */
    def isSupportedType(v: V): Boolean =
        if (v.value.isPrimitiveValue) {
            isSupportedType(v.value.asPrimitiveValue.primitiveType.toJava)
        } else {
            try {
                isSupportedType(v.value.verificationTypeInfo.asObjectVariableInfo.clazz.toJava)
            } catch {
                case _: Exception => false
            }
        }

    def isSupportedType(fieldType: FieldType): Boolean = isSupportedType(fieldType.toJava)
}
