/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
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

    override val apiFeatures: List[APIFeature] = {

        val Unsafe = ObjectType("sun/misc/Unsafe")

        List(

            StaticAPIMethod(Unsafe, "getUnsafe", MethodDescriptor("()Lsun/misc/Unsafe;")),

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Unsafe, "allocateInstance")
                ),
                "Unsafe - Alloc"
            ),

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Unsafe, "arrayIndexScale"),
                    InstanceAPIMethod(Unsafe, "arrayBaseOffset")
                ),
                "Unsafe - Array"
            ),

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Unsafe, "compareAndSwapObject"),
                    InstanceAPIMethod(Unsafe, "compareAndSwapInt"),
                    InstanceAPIMethod(Unsafe, "compareAndSwapLong")
                ),
                "Unsafe - compareAndSwap"
            ),

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Unsafe, "shouldBeInitialized"),
                    InstanceAPIMethod(Unsafe, "defineAnonymousClass"),
                    InstanceAPIMethod(Unsafe, "ensureClassInitialized"),
                    InstanceAPIMethod(Unsafe, "defineClass")
                ),
                "Unsafe - Class"
            ),

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Unsafe, "loadFence"),
                    InstanceAPIMethod(Unsafe, "storeFence"),
                    InstanceAPIMethod(Unsafe, "fullFence")
                ),
                "Unsafe - Fence"
            ),

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Unsafe, "getAndSetObject"),
                    InstanceAPIMethod(Unsafe, "getAndSetLong"),
                    InstanceAPIMethod(Unsafe, "getAndSetInt"),
                    InstanceAPIMethod(Unsafe, "getAndAddLong"),
                    InstanceAPIMethod(Unsafe, "getAndAddInt")
                ),
                "Unsafe - Fetch & Add"
            ),

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Unsafe, "setMemory"),
                    InstanceAPIMethod(Unsafe, "copyMemory")
                ),
                "Unsafe - Heap"
            ),

            APIFeatureGroup(
                List(
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
                List(
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

            APIFeatureGroup(List(InstanceAPIMethod(Unsafe, "getLoadAverage")), "Misc"),

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Unsafe, "tryMonitorEnter"),
                    InstanceAPIMethod(Unsafe, "monitorExit"),
                    InstanceAPIMethod(Unsafe, "monitorEnter")
                ),
                "Unsafe - Monitor"
            ),

            APIFeatureGroup(
                List(
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
                List(
                    InstanceAPIMethod(Unsafe, "fieldOffset"),
                    InstanceAPIMethod(Unsafe, "staticFieldOffset"),
                    InstanceAPIMethod(Unsafe, "staticFieldBase"),
                    InstanceAPIMethod(Unsafe, "objectFieldOffset")
                ),
                "Unsafe - Offset"
            ),

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Unsafe, "putOrderedObject"),
                    InstanceAPIMethod(Unsafe, "putOrderedLong"),
                    InstanceAPIMethod(Unsafe, "putOrderedInt")
                ),
                "Unsafe - Ordered Put"
            ),

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Unsafe, "park"),
                    InstanceAPIMethod(Unsafe, "unpark")
                ),
                "Unsafe - Park"
            ),

            APIFeatureGroup(List(InstanceAPIMethod(Unsafe, "throwException")), "Unsafe - Throw"),

            APIFeatureGroup(
                List(
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
                List(
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
