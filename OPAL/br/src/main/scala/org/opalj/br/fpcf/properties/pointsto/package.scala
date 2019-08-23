/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.br.analyses.DeclaredMethods

package object pointsto {
    // we encode allocation sites (method, pc, emptyArray, typeid tuples) as longs
    type AllocationSite = Long

    @inline def allocationSiteToLong(
        method:       DeclaredMethod,
        pc:           Int,
        tpe:          ReferenceType,
        isEmptyArray: Boolean        = false
    ): Long = {
        val methodId = method.id
        val typeId = tpe.id
        val emptyArray = if (isEmptyArray) 1L else 0L
        assert(pc >= 0 && pc <= 0xFFFF)
        assert(methodId >= 0 && methodId <= 0x3FFFFF)
        assert(typeId >= -0x1000000 && typeId <= 0x1FFFFFF)
        methodId.toLong | (pc.toLong << 22) | (emptyArray << 38) | (typeId.toLong << 39)
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
            (encodedAllocationSite >> 39).toInt
        )
    }

    @inline def allocationSiteLongToTypeId(encodedAllocationSite: AllocationSite): Int = {
        (encodedAllocationSite >> 39).toInt
    }

    @inline def isEmptyArrayAllocationSite(encodedAllocationSite: AllocationSite): Boolean = {
        (encodedAllocationSite >> 38) % 2 == -1
    }
}
