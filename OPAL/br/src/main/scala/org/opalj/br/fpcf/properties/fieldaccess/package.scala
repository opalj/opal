/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import scala.collection.immutable.IntMap

import org.opalj.value.ValueInformation

package object fieldaccess {
    // we encode field accesses (method, pc) as longs
    // MSB 32 bit PC (if space is needed, reduce this) | 32 bit ContextId LSB
    type FieldAccess = Long

    type AccessReceiver = Option[(ValueInformation, PCs)]
    type AccessParameter = Option[(ValueInformation, PCs)]

    type AccessReceivers = IntMap[IntMap[AccessReceiver]] // Access Context => PC => Receiver
    type AccessParameters = IntMap[IntMap[AccessParameter]] // Access Context => PC => Parameter

    @inline private[fieldaccess] def encodeFieldAccess(contextId: Int, pc: Int): FieldAccess = {
        contextId.toLong | (pc.toLong << 32)
    }

    @inline private[fieldaccess] def decodeFieldAccess(fieldAccess: FieldAccess): (Int, Int) = {
        val contextId = fieldAccess.toInt
        val pc = (fieldAccess >> 32).toInt
        (contextId, pc)
    }
}
