/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import org.opalj.br.MethodDescriptor
import org.opalj.br.MethodDescriptor.JustReturnsObject
import org.opalj.br.ObjectType
import org.opalj.hermes.queries.util.APIFeature
import org.opalj.hermes.queries.util.APIFeatureQuery
import org.opalj.hermes.queries.util.APIFeatureGroup
import org.opalj.hermes.queries.util.InstanceAPIMethod
import org.opalj.hermes.queries.util.StaticAPIMethod

/**
 * Counts the number of certain calls to the Java Reflection API.
 *
 * @author Michael Reif
 */
class ReflectionAPIUsage(implicit hermes: HermesConfig) extends APIFeatureQuery {

    override val apiFeatures: List[APIFeature] = {

        val Class = ObjectType.Class
        val Field = ObjectType("java/lang/reflect/Field")
        val AccessibleObject = ObjectType("java/lang/reflect/AccessibleObject")
        val Constructor = ObjectType("java/lang/reflect/Constructor")
        val Method = ObjectType("java/lang/reflect/Method")
        val MethodHandle = ObjectType("java/lang/invoke/MethodHandle")
        val MethodHandles = ObjectType("java/lang/invoke/MethodHandles")
        // TODO val MethodHandles_Lookup = ObjectType("java/lang/invoke/MethodHandles$Lookup")
        val Proxy = ObjectType("java/lang/reflect/Proxy")

        List(

            StaticAPIMethod(Class, "forName"),

            // reflective instance creation
            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Class, "newInstance", JustReturnsObject),
                    InstanceAPIMethod(Constructor, "newInstance", "([Ljava/lang/Object;)Ljava/lang/Object;")
                ),
                "reflective instance creation"
            ),

            // reflective field write api
            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Field, "set", "(Ljava/lang/Object;Ljava/lang/Object;)V"),
                    InstanceAPIMethod(Field, "setBoolean", "(Ljava/lang/Object;Z)V"),
                    InstanceAPIMethod(Field, "setByte", "(Ljava/lang/Object;B)V"),
                    InstanceAPIMethod(Field, "setChar", "(Ljava/lang/Object;C)V"),
                    InstanceAPIMethod(Field, "setDouble", "(Ljava/lang/Object;D)V"),
                    InstanceAPIMethod(Field, "setFloat", "(Ljava/lang/Object;F)V"),
                    InstanceAPIMethod(Field, "setInt", "(Ljava/lang/Object;I)V"),
                    InstanceAPIMethod(Field, "setLong", "(Ljava/lang/Object;J)V"),
                    InstanceAPIMethod(Field, "setShort", "(Ljava/lang/Object;S)V")
                ),
                "reflective field write"
            ),

            // reflective field read api
            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Field, "get"),
                    InstanceAPIMethod(Field, "getBoolean"),
                    InstanceAPIMethod(Field, "getByte"),
                    InstanceAPIMethod(Field, "getChar"),
                    InstanceAPIMethod(Field, "getDouble"),
                    InstanceAPIMethod(Field, "getFloat"),
                    InstanceAPIMethod(Field, "getInt"),
                    InstanceAPIMethod(Field, "getLong"),
                    InstanceAPIMethod(Field, "getShort")
                ),
                "reflective field read"
            ),

            // making fields accessible using "setAccessible"
            APIFeatureGroup(
                List(
                    InstanceAPIMethod(
                        Field, "setAccessible", s"([${AccessibleObject.toJVMTypeName}Z)V"
                    ),
                    InstanceAPIMethod(Field, "setAccessible", "(Z)V")
                ),
                "makes fields accessible"
            ),

            // setting methods or constructors accessible
            APIFeatureGroup(
                List(
                    InstanceAPIMethod(
                        Method, "setAccessible", s"([${AccessibleObject.toJVMTypeName}Z)V"
                    ),
                    InstanceAPIMethod(Method, "setAccessible", MethodDescriptor("(Z)V")),
                    InstanceAPIMethod(
                        Constructor, "setAccessible", s"([${AccessibleObject.toJVMTypeName}Z)V"
                    ),
                    InstanceAPIMethod(Constructor, "setAccessible", MethodDescriptor("(Z)V"))
                ),
                "makes methods or constructors accessible"
            ),

            // set an AccessibleObject accessible
            APIFeatureGroup(
                List(
                    InstanceAPIMethod(
                        AccessibleObject, "setAccessible", s"([${AccessibleObject.toJVMTypeName}Z)V"
                    ),
                    InstanceAPIMethod(AccessibleObject, "setAccessible", "(Z)V")
                ),
                "makes an AccessibleObject accessible\n(exact type unknown)"
            ),

            // reflective method invocation
            InstanceAPIMethod(
                Method,
                "invoke",
                MethodDescriptor(s"(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;")
            ),

            //////////////////////////////////////////////////////////////////////////////////////////////
            //////////////////////////////// new Reflection primitives ///////////////////////////////////
            //////////////////////////////////////////////////////////////////////////////////////////////

            StaticAPIMethod(MethodHandles, "lookup"),
            StaticAPIMethod(MethodHandles, "publicLookup"),

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(MethodHandle, "invokeExact"),
                    InstanceAPIMethod(MethodHandle, "invoke"),
                    InstanceAPIMethod(MethodHandle, "invokeWithArguments")
                ),
                "method handle invocation"
            ),

            StaticAPIMethod(Proxy, "newProxyInstance")
        )
    }
}
