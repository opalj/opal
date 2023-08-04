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
    // MSB 16 bit unused | 16 bit PC | 32 bit MethodID LSB
    type FieldAccess = Long

    type AccessReceiver = Option[(ValueInformation, PCs)]
    type AccessParameter = Option[(ValueInformation, PCs)]

    type IndirectAccessReceivers = IntMap[IntMap[AccessReceiver]] // Caller Context => PC => Receiver
    type IndirectAccessParameters = IntMap[IntMap[AccessParameter]] // Caller Context => PC => Parameter

    @inline def encodeDirectFieldAccess(method: DefinedMethod, pc: Int): FieldAccess = {
        method.id.toLong | (pc.toLong << 31)
    }

    @inline def encodeIndirectFieldAccess(contextId: Int, pc: Int): FieldAccess = {
        contextId.toLong | (pc.toLong << 31)
    }

    @inline def decodeDirectFieldAccess(fieldAccess: FieldAccess)(implicit declaredMethods: DeclaredMethods): (DefinedMethod, Int) = {
        val methodId = fieldAccess.toInt
        val pc = (fieldAccess >> 31).toInt
        (declaredMethods(methodId).asDefinedMethod, pc)
    }

    @inline def decodeIndirectFieldAccess(fieldAccess: FieldAccess): (Int, Int) = {
        val methodId = fieldAccess.toInt
        val pc = (fieldAccess >> 31).toInt
        (methodId, pc)
    }
}