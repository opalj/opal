/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.br.analyses.DeclaredMethods

package object pointsto {
    // we encode allocation sites (method, pc tuples) as longs
    type AllocationSite = Long

    def allocationSiteToLong(method: DeclaredMethod, pc: Int): Long = {
        val methodId = method.id
        assert(pc >= 0 && pc <= 0xFFFF)
        assert(methodId >= 0 && methodId <= 0x3FFFFF)
        methodId.toLong | (pc.toLong << 22)
    }

    def longToAllocationSite(
        encodedAllocationSite: AllocationSite
    )(implicit declaredMethods: DeclaredMethods): (DeclaredMethod, Int) = {
        (
            declaredMethods(encodedAllocationSite.toInt & 0x3FFFFF),
            (encodedAllocationSite >> 22).toInt & 0xFFFF
        )
    }
}
