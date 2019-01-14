/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
