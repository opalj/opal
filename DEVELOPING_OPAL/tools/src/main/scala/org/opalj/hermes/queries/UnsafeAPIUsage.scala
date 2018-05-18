/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package hermes
package queries

import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.collection.immutable.Chain
import org.opalj.hermes.queries.util.APIFeature
import org.opalj.hermes.queries.util.APIFeatureQuery
import org.opalj.hermes.queries.util.APIFeatureGroup
import org.opalj.hermes.queries.util.InstanceAPIMethod
import org.opalj.hermes.queries.util.StaticAPIMethod

/**
 * Groups features that rely on the Unsafe API. (sun.misc.Unsafe)
 *
 * @note Feature groups are taken from and are further discussed in the following paper:
 *       "Use at Your Own Risk: The Java Unsafe API in the Wild"
 *       by Luis Mastrangelo et al.
 *
 * @author Michael Reif
 */
class UnsafeAPIUsage(implicit hermes: HermesConfig) extends APIFeatureQuery {

    override val apiFeatures: Chain[APIFeature] = {

        val Unsafe = ObjectType("sun/misc/Unsafe")

        Chain(

            StaticAPIMethod(Unsafe, "getUnsafe", MethodDescriptor("()Lsun/misc/Unsafe;")),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Unsafe, "allocateInstance")
                ),
                "Unsafe - Alloc"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Unsafe, "arrayIndexScale"),
                    InstanceAPIMethod(Unsafe, "arrayBaseOffset")
                ),
                "Unsafe - Array"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Unsafe, "compareAndSwapObject"),
                    InstanceAPIMethod(Unsafe, "compareAndSwapInt"),
                    InstanceAPIMethod(Unsafe, "compareAndSwapLong")
                ),
                "Unsafe - compareAndSwap"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Unsafe, "shouldBeInitialized"),
                    InstanceAPIMethod(Unsafe, "defineAnonymousClass"),
                    InstanceAPIMethod(Unsafe, "ensureClassInitialized"),
                    InstanceAPIMethod(Unsafe, "defineClass")
                ),
                "Unsafe - Class"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Unsafe, "loadFence"),
                    InstanceAPIMethod(Unsafe, "storeFence"),
                    InstanceAPIMethod(Unsafe, "fullFence")
                ),
                "Unsafe - Fence"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Unsafe, "getAndSetObject"),
                    InstanceAPIMethod(Unsafe, "getAndSetLong"),
                    InstanceAPIMethod(Unsafe, "getAndSetInt"),
                    InstanceAPIMethod(Unsafe, "getAndAddLong"),
                    InstanceAPIMethod(Unsafe, "getAndAddInt")
                ),
                "Unsafe - Fetch & Add"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Unsafe, "setMemory"),
                    InstanceAPIMethod(Unsafe, "copyMemory")
                ),
                "Unsafe - Heap"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Unsafe, "getByte"),
                    InstanceAPIMethod(Unsafe, "getShort"),
                    InstanceAPIMethod(Unsafe, "getChar"),
                    InstanceAPIMethod(Unsafe, "getInt"),
                    InstanceAPIMethod(Unsafe, "getLong"),
                    InstanceAPIMethod(Unsafe, "getFloat"),
                    InstanceAPIMethod(Unsafe, "getDouble"),
                    InstanceAPIMethod(Unsafe, "getObject"),
                    InstanceAPIMethod(Unsafe, "getBoolean")
                ),
                "Unsafe - Heap Get"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Unsafe, "putByte"),
                    InstanceAPIMethod(Unsafe, "putShort"),
                    InstanceAPIMethod(Unsafe, "putChar"),
                    InstanceAPIMethod(Unsafe, "putInt"),
                    InstanceAPIMethod(Unsafe, "putLong"),
                    InstanceAPIMethod(Unsafe, "putFloat"),
                    InstanceAPIMethod(Unsafe, "putDouble"),
                    InstanceAPIMethod(Unsafe, "putObject"),
                    InstanceAPIMethod(Unsafe, "putBoolean")
                ),
                "Unsafe - Heap Put"
            ),

            APIFeatureGroup(Chain(InstanceAPIMethod(Unsafe, "getLoadAverage")), "Misc"),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Unsafe, "tryMonitorEnter"),
                    InstanceAPIMethod(Unsafe, "monitorExit"),
                    InstanceAPIMethod(Unsafe, "monitorEnter")
                ),
                "Unsafe - Monitor"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Unsafe, "setMemory"),
                    InstanceAPIMethod(Unsafe, "reallocateMemory"),
                    InstanceAPIMethod(Unsafe, "putAddress"),
                    InstanceAPIMethod(Unsafe, "pageSize"),
                    InstanceAPIMethod(Unsafe, "getAddress"),
                    InstanceAPIMethod(Unsafe, "freeMemory"),
                    InstanceAPIMethod(Unsafe, "copyMemory"),
                    InstanceAPIMethod(Unsafe, "addressSize")
                ),
                "Unsafe - Off-Heap"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Unsafe, "fieldOffset"),
                    InstanceAPIMethod(Unsafe, "staticFieldOffset"),
                    InstanceAPIMethod(Unsafe, "staticFieldBase"),
                    InstanceAPIMethod(Unsafe, "objectFieldOffset")
                ),
                "Unsafe - Offset"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Unsafe, "putOrderedObject"),
                    InstanceAPIMethod(Unsafe, "putOrderedLong"),
                    InstanceAPIMethod(Unsafe, "putOrderedInt")
                ),
                "Unsafe - Ordered Put"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Unsafe, "park"),
                    InstanceAPIMethod(Unsafe, "unpark")
                ),
                "Unsafe - Park"
            ),

            APIFeatureGroup(Chain(InstanceAPIMethod(Unsafe, "throwException")), "Unsafe - Throw"),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Unsafe, "getByteVolatile"),
                    InstanceAPIMethod(Unsafe, "getShortVolatile"),
                    InstanceAPIMethod(Unsafe, "getCharVolatile"),
                    InstanceAPIMethod(Unsafe, "getIntVolatile"),
                    InstanceAPIMethod(Unsafe, "getLongVolatile"),
                    InstanceAPIMethod(Unsafe, "getFloatVolatile"),
                    InstanceAPIMethod(Unsafe, "getDoubleVolatile"),
                    InstanceAPIMethod(Unsafe, "getObjectVolatile"),
                    InstanceAPIMethod(Unsafe, "getBooleanVolatile")
                ),
                "Unsafe - Volatile Get"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Unsafe, "putByteVolatile"),
                    InstanceAPIMethod(Unsafe, "putShortVolatile"),
                    InstanceAPIMethod(Unsafe, "putCharVolatile"),
                    InstanceAPIMethod(Unsafe, "putIntVolatile"),
                    InstanceAPIMethod(Unsafe, "putLongVolatile"),
                    InstanceAPIMethod(Unsafe, "putFloatVolatile"),
                    InstanceAPIMethod(Unsafe, "putDoubleVolatile"),
                    InstanceAPIMethod(Unsafe, "putObjectVolatile"),
                    InstanceAPIMethod(Unsafe, "putBooleanVolatile")
                ),
                "Unsafe - Volatile Put"
            )
        )
    }
}
