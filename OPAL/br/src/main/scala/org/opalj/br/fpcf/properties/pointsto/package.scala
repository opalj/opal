/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.br.fpcf.analyses.ContextProvider

package object pointsto {
    // we encode allocation sites (method, pc, emptyArray, typeid tuples) as longs
    // MSB 20 bit TypeID | 1 bit is empty array | 16 bit PC | 27 bit ContextID LSB
    type AllocationSite = Long

    @inline def allocationSiteToLong(
        context:      Context,
        pc:           Int,
        tpe:          ReferenceType,
        isEmptyArray: Boolean = false): Long = {
        val contextId  = if (context eq NoContext) 0x3ffffff else context.id
        val typeId     = tpe.id
        val emptyArray = if (isEmptyArray) 1L else 0L
        assert(pc >= -0x10000 && pc <= 0xffff)
        assert(contextId >= 0 && contextId <= 0x3ffffff)
        assert(typeId >= -0x80000 && typeId <= 0x7ffff)
        contextId.toLong | ((pc.toLong & 0x1ffff) << 26) | (emptyArray << 43) | (typeId.toLong << 44)
    }

    @inline def allocationSiteLongToTypeId(encodedAllocationSite: AllocationSite): Int =
        (encodedAllocationSite >> 44).toInt

    @inline def isEmptyArrayAllocationSite(encodedAllocationSite: AllocationSite): Boolean =
        (encodedAllocationSite >> 43) % 2 == -1

    @inline def longToAllocationSite(
        encodedAllocationSite: AllocationSite
      )(implicit
        contextProvider: ContextProvider): (Context, PC, Int) /* method, pc, typeid */ = {
        val contextID = encodedAllocationSite.toInt & 0x3ffffff
        val pc        = (encodedAllocationSite >> 26).toInt & 0x1ffff
        (
            contextProvider.contextFromId(if (contextID == 0x3ffffff) -1 else contextID),
            if (pc > 0xffff) pc | 0xffff0000 else pc,
            (encodedAllocationSite >> 44).toInt
        )
    }
}
