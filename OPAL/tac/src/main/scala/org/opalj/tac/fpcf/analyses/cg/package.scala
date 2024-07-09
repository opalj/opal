/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.ai.ImmediateVMExceptionsOriginOffset
import org.opalj.ai.MethodExternalExceptionsOriginOffset
import org.opalj.ai.ValueOrigin
import org.opalj.ai.ValueOriginForImmediateVMException
import org.opalj.ai.ValueOriginForMethodExternalException
import org.opalj.ai.isImmediateVMException
import org.opalj.ai.isMethodExternalExceptionOrigin
import org.opalj.ai.pcOfImmediateVMException
import org.opalj.ai.pcOfMethodExternalException
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.DeclaredMethod
import org.opalj.br.ObjectType
import org.opalj.br.PCs
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
import org.opalj.collection.immutable.EmptyIntTrieSet
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.UIDSet
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.logOnce
import org.opalj.log.Warn
import org.opalj.value.ValueInformation

package object cg {

    /**
     * A persistent representation (using pcs instead of TAC value origins) for a UVar.
     */
    final def persistentUVar(
        value: V
    )(
        implicit stmts: Array[Stmt[V]]
    ): Some[(ValueInformation, IntTrieSet)] = {
        Some((value.value, value.definedBy.map(pcOfDefSite _)))
    }

    final def pcOfDefSite(valueOrigin: ValueOrigin)(implicit stmts: Array[Stmt[V]]): Int = {
        if (valueOrigin >= 0)
            stmts(valueOrigin).pc
        else if (valueOrigin > ImmediateVMExceptionsOriginOffset)
            valueOrigin // <- it is a parameter!
        else if (valueOrigin > MethodExternalExceptionsOriginOffset)
            ValueOriginForImmediateVMException(stmts(pcOfImmediateVMException(valueOrigin)).pc)
        else
            ValueOriginForMethodExternalException(
                stmts(pcOfMethodExternalException(valueOrigin)).pc
            )
    }

    final def valueOriginsOfPCs(pcs: PCs, pcToIndex: Array[Int]): IntTrieSet = {
        pcs.foldLeft(EmptyIntTrieSet: IntTrieSet) { (origins, pc) =>
            if (ai.underlyingPC(pc) < 0)
                origins + pc // parameter
            else if (pc >= 0 && pcToIndex(pc) >= 0)
                origins + pcToIndex(pc) // local
            else if (isImmediateVMException(pc) && pcToIndex(pcOfImmediateVMException(pc)) >= 0)
                origins + ValueOriginForImmediateVMException(pcToIndex(pcOfImmediateVMException(pc)))
            else if (isMethodExternalExceptionOrigin(pc) && pcToIndex(pcOfMethodExternalException(pc)) >= 0)
                origins + ValueOriginForMethodExternalException(pcToIndex(pcOfMethodExternalException(pc)))
            else
                origins // as is
        }
    }

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
                            case _: LoadClass | _: LoadClass_W               => constantTypes += ObjectType.Class
                            case _: LoadMethodHandle | _: LoadMethodHandle_W => constantTypes += ObjectType.MethodHandle
                            case _: LoadMethodType | _: LoadMethodType_W     => constantTypes += ObjectType.MethodType
                            case _: LoadString | _: LoadString_W             => constantTypes += ObjectType.String
                            case _: LoadDynamic =>
                                constantTypes += inst.asInstanceOf[LoadDynamic].descriptor.asReferenceType
                            case _: LoadDynamic_W =>
                                constantTypes += inst.asInstanceOf[LoadDynamic_W].descriptor.asReferenceType
                            case ACONST_NULL =>
                            case _ =>
                                logOnce(Warn("unknown load constant instruction"))
                        }
                    }
                }
            }
        }
        constantTypes
    }
}
