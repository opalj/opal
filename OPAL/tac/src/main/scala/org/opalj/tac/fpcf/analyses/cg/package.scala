/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.br.ClassType
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.DeclaredMethod
import org.opalj.br.ReferenceType
import org.opalj.br.instructions.ACONST_NULL
import org.opalj.br.instructions.LoadClass
import org.opalj.br.instructions.LoadClass_W
import org.opalj.br.instructions.LoadConstantInstruction
import org.opalj.br.instructions.LoadDynamic
import org.opalj.br.instructions.LoadDynamic_W
import org.opalj.br.instructions.LoadMethodHandle
import org.opalj.br.instructions.LoadMethodHandle_W
import org.opalj.br.instructions.LoadMethodType
import org.opalj.br.instructions.LoadMethodType_W
import org.opalj.br.instructions.LoadString
import org.opalj.br.instructions.LoadString_W
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.UIDSet
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.logOnce
import org.opalj.log.Warn
import org.opalj.value.ValueInformation

package object cg {

    /**
     * A persistent representation (using pcs instead of TAC value origins) for a UVar.
     *
     * @deprecated Use [[UVar.toPersistentForm]] (available through the type [[V]]) instead.
     */
    final def persistentUVar(
        value: V
    )(
        implicit stmts: Array[Stmt[V]]
    ): Some[(ValueInformation, IntTrieSet)] = {
        Some((value.value, value.definedBy.map(pcOfDefSite _)))
    }

    /**
     * Regaining a non-persistent (using TAC value origins) form of the UVar.
     *
     * @deprecated Use [[PUVar.toValueOriginForm]] instead.
     */
    final def uVarForDefSites(
        defSites:  (ValueInformation, IntTrieSet),
        pcToIndex: Array[Int]
    ): V = {
        UVar(defSites._1, valueOriginsOfPCs(defSites._2, pcToIndex))
    }

    private[cg] def getLoadConstantTypes(
        method: DeclaredMethod
    )(implicit logContext: LogContext): UIDSet[ReferenceType] = {
        var constantTypes = UIDSet.empty[ReferenceType]
        if (method.hasSingleDefinedMethod || method.hasMultipleDefinedMethods) {
            method.foreachDefinedMethod { m =>
                for {
                    code <- m.body
                    inst <- code.instructions
                } {
                    if ((inst ne null) && inst.isLoadConstantInstruction &&
                        inst.asInstanceOf[LoadConstantInstruction[_]].computationalType ==
                            ComputationalTypeReference
                    ) {
                        inst match {
                            case _: LoadClass | _: LoadClass_W               => constantTypes += ClassType.Class
                            case _: LoadMethodHandle | _: LoadMethodHandle_W => constantTypes += ClassType.MethodHandle
                            case _: LoadMethodType | _: LoadMethodType_W     => constantTypes += ClassType.MethodType
                            case _: LoadString | _: LoadString_W             => constantTypes += ClassType.String
                            case _: LoadDynamic                              =>
                                constantTypes += inst.asInstanceOf[LoadDynamic].descriptor.asReferenceType
                            case _: LoadDynamic_W =>
                                constantTypes += inst.asInstanceOf[LoadDynamic_W].descriptor.asReferenceType
                            case ACONST_NULL =>
                            case _           =>
                                logOnce(Warn("unknown load constant instruction"))
                        }
                    }
                }
            }
        }
        constantTypes
    }
}
