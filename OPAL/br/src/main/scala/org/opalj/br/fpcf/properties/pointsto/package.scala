/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.br.analyses.DeclaredMethods

package object pointsto {
    // we encode allocation sites (method, pc, typeid tuples) as longs
    type AllocationSite = Long

    @inline def allocationSiteToLong(method: DeclaredMethod, pc: Int, tpe: ReferenceType): Long = {
        val methodId = method.id
        val typeId = tpe.id
        assert(pc >= 0 && pc <= 0xFFFF)
        assert(methodId >= 0 && methodId <= 0x3FFFFF)
        assert(typeId >= -0x2000000 && typeId <= 0x3FFFFFF)
        methodId.toLong | (pc.toLong << 22) | (typeId.toLong << 38)
    }

    @inline def longToAllocationSite(
        encodedAllocationSite: AllocationSite
    )(
        implicit
        declaredMethods: DeclaredMethods
    ): (DeclaredMethod, PC, Int) /* method, pc, typeid */ = {
        (
            declaredMethods(encodedAllocationSite.toInt & 0x3FFFFF),
            (encodedAllocationSite >> 22).toInt & 0xFFFF,
            (encodedAllocationSite >> 38).toInt
        )
    }

    @inline def allocationSiteLongToTypeId(encodedAllocationSite: AllocationSite): Int = {
        (encodedAllocationSite >> 38).toInt
    }
}
