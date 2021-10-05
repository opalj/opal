/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package escape

import org.opalj.collection.immutable.EmptyIntTrieSet
import org.opalj.collection.immutable.IntRefPair
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.EscapeViaHeapObject
import org.opalj.br.fpcf.properties.EscapeViaParameter
import org.opalj.br.fpcf.properties.NoEscape

/**
 * Very simple handling for fields and arrays. This analysis can detect global escapes via
 * assignments to heap objects. Due to the lack of simple may-alias analysis, this analysis can not
 * determine [[org.opalj.br.fpcf.properties.NoEscape]] states.
 *
 * @author Florian Kuebler
 */
trait SimpleFieldAwareEscapeAnalysis extends AbstractEscapeAnalysis {

    override protected[this] def handlePutField(
        putField: PutField[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(putField.value))
            handleFieldLike(putField.objRef.asVar.definedBy)
    }

    override protected[this] def handleArrayStore(
        arrayStore: ArrayStore[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (state.usesDefSite(arrayStore.value))
            handleFieldLike(arrayStore.arrayRef.asVar.definedBy)
    }

    /**
     * A worklist algorithm, check the def sites of the reference of the field, or array, to which
     * the current entity was assigned.
     */
    private[this] def handleFieldLike(
        referenceDefSites: IntTrieSet
    )(implicit state: AnalysisState): Unit = {
        // the definition sites to handle
        var workset = referenceDefSites

        // the definition sites that were already handled
        var seen: IntTrieSet = EmptyIntTrieSet

        while (workset.nonEmpty) {
            val IntRefPair(referenceDefSite, newWorklist) = workset.headAndTail
            workset = newWorklist
            seen += referenceDefSite

            // do not check the escape state of the entity (defSite) whose escape state we are
            // currently computing to avoid endless loops
            if (state.defSite != referenceDefSite) {
                // is the object/array reference of the field a local
                if (referenceDefSite >= 0) {
                    state.tacai.get.stmts(referenceDefSite) match {
                        case Assignment(_, _, New(_, _) | NewArray(_, _, _)) =>
                            /* as may alias information are not easily available we cannot simply
                            check for escape information of the base object */
                            state.meetMostRestrictive(AtMost(NoEscape))
                        /*val allocationSites =
                                propertyStore.context[AllocationSites]
                            val allocationSite = allocationSites(m)(pc)
                            val escapeState = propertyStore(allocationSite, EscapeProperty.key)
                            escapeState match {
                                case EP(_, EscapeViaStaticField) => calcMostRestrictive(EscapeViaHeapObject)
                                case EP(_, EscapeViaHeapObject)  => calcMostRestrictive(EscapeViaStaticField)
                                case EP(_, GlobalEscape)         => calcMostRestrictive(GlobalEscape)
                                case EP(_, NoEscape)             => calcMostRestrictive(NoEscape)
                                case EP(_, p) if p.isFinal       => calcMostRestrictive(MaybeNoEscape)
                                case _                           => dependees += escapeState

                            }*/
                        /* if the base object came from a static field, the value assigned to it
                         escapes globally */
                        case Assignment(_, _, GetStatic(_, _, _, _)) =>
                            state.meetMostRestrictive(EscapeViaHeapObject)
                        case Assignment(_, _, GetField(_, _, _, _, objRef)) =>
                            objRef.asVar.definedBy foreach { x =>
                                if (!seen.contains(x))
                                    workset += x
                            }
                        case Assignment(_, _, ArrayLoad(_, _, arrayRef)) =>
                            arrayRef.asVar.definedBy foreach { x =>
                                if (!seen.contains(x))
                                    workset += x
                            }
                        // we are not inter-procedural
                        case Assignment(_, _, _: FunctionCall[_]) =>
                            state.meetMostRestrictive(AtMost(NoEscape))
                        case Assignment(_, _, _: Const) =>
                        case s =>
                            throw new UnknownError(s"Unexpected tac: $s")
                    }

                } else if (referenceDefSite > ai.ImmediateVMExceptionsOriginOffset) {
                    // assigned to field of parameter
                    state.meetMostRestrictive(AtMost(EscapeViaParameter))
                    /* As may alias information are not easily available we cannot simply use
                    the code below:
                    val formalParameters = propertyStore.context[FormalParameters]
                    val formalParameter = formalParameters(m)(-referenceDefSite - 1)
                    val escapeState = propertyStore(formalParameter, EscapeProperty.key)
                    escapeState match {
                        case EP(_, p: GlobalEscape) => calcLeastRestrictive(p)
                        case EP(_, p) if p.isFinal  =>
                        case _                      => dependees += escapeState
                    }*/
                } else {
                    // we store the value into a field of an exception object. As we do not track
                    // the field any further we are done.
                    state.meetMostRestrictive(AtMost(NoEscape))
                }
            }
        }
    }
}
