/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.br.analyses.DeclaredMethods

package object fieldaccess {
    // we encode field accesses (method, pc) as longs
    // MSB 16 bit unused | 16 bit PC | 32 bit MethodID LSB
    type FieldAccess = Long

    @inline def encodeFieldAccess(method: DefinedMethod, pc: Int): FieldAccess = {
        method.id.toLong | (pc.toLong << 31)
    }

    @inline def decodeFieldAccess(fieldAccess: FieldAccess)(implicit declaredMethods: DeclaredMethods): (DefinedMethod, Int) = {
        val methodId = fieldAccess.toInt
        val pc = (fieldAccess >> 31).toInt
        (declaredMethods(methodId).asDefinedMethod, pc)
    }
}