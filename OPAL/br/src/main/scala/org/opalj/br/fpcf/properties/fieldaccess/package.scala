/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.br.analyses.DeclaredMethods
import org.opalj.value.ValueInformation

import scala.collection.immutable.IntMap

package object fieldaccess {
    // we encode field accesses (method, pc) as longs
    // MSB 32 bit PC (if space is needed, reduce this) | 32 bit ContextId LSB
    type FieldAccess = Long

    type AccessReceiver = Option[(ValueInformation, PCs)]
    type AccessParameter = Option[(ValueInformation, PCs)]

    type IndirectAccessReceivers = IntMap[IntMap[AccessReceiver]] // Access Context => PC => Receiver
    type IndirectAccessParameters = IntMap[IntMap[AccessParameter]] // Access Context => PC => Parameter

    @inline def encodeDirectFieldAccess(method: DefinedMethod, pc: Int): FieldAccess = {
        method.id.toLong | (pc.toLong << 32)
    }

    @inline def encodeIndirectFieldAccess(contextId: Int, pc: Int): FieldAccess = {
        contextId.toLong | (pc.toLong << 32)
    }

    @inline def decodeDirectFieldAccess(
        fieldAccess: FieldAccess
    )(implicit declaredMethods: DeclaredMethods): (DefinedMethod, Int) = {
        val methodId = fieldAccess.toInt
        val pc = (fieldAccess >> 32).toInt
        (declaredMethods(methodId).asDefinedMethod, pc)
    }

    @inline def decodeIndirectFieldAccess(fieldAccess: FieldAccess): (Int, Int) = {
        val contextId = fieldAccess.toInt
        val pc = (fieldAccess >> 32).toInt
        (contextId, pc)
    }
}