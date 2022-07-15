/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

package object pointsto {
    // we encode allocation sites (method, pc, emptyArray, typeid tuples) as longs
    // MSB 20 bit TypeID | 1 bit is empty array | 16 bit PC | 27 bit ContextID LSB
    type AllocationSite = Long

    @inline def allocationSiteToLong(
        context:      Context,
        pc:           Int,
        tpe:          ReferenceType,
        isEmptyArray: Boolean       = false
    ): Long = {
        val contextId = if (context eq NoContext) 0x7FFFFFF else context.id
        val typeId = tpe.id
        val emptyArray = if (isEmptyArray) 1L else 0L
        assert(pc >= 0 && pc <= 0xFFFF)
        assert(contextId >= 0 && contextId <= 0x7FFFFFF)
        assert(typeId >= -0x80000 && typeId <= 0x7FFFF)
        contextId.toLong | (pc.toLong << 27) | (emptyArray << 43) | (typeId.toLong << 44)
    }

    @inline def allocationSiteLongToTypeId(encodedAllocationSite: AllocationSite): Int = {
        (encodedAllocationSite >> 44).toInt
    }

    @inline def isEmptyArrayAllocationSite(encodedAllocationSite: AllocationSite): Boolean = {
        (encodedAllocationSite >> 43) % 2 == -1
    }
}
