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
import org.opalj.br.MethodDescriptor.JustReturnsObject
import org.opalj.br.ObjectType
import org.opalj.collection.immutable.Chain
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

    override val apiFeatures: Chain[APIFeature] = {

        val Class = ObjectType.Class
        val Field = ObjectType("java/lang/reflect/Field")
        val AccessibleObject = ObjectType("java/lang/reflect/AccessibleObject")
        val Constructor = ObjectType("java/lang/reflect/Constructor")
        val Method = ObjectType("java/lang/reflect/Method")
        val MethodHandle = ObjectType("java/lang/invoke/MethodHandle")
        val MethodHandles = ObjectType("java/lang/invoke/MethodHandles")
        // TODO val MethodHandles_Lookup = ObjectType("java/lang/invoke/MethodHandles$Lookup")
        val Proxy = ObjectType("java/lang/reflect/Proxy")

        Chain(

            StaticAPIMethod(Class, "forName"),

            // reflective instance creation
            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Class, "newInstance", JustReturnsObject),
                    InstanceAPIMethod(Constructor, "newInstance", "([Ljava/lang/Object;)Ljava/lang/Object;")
                ),
                "reflective instance creation"
            ),

            // reflective field write api
            APIFeatureGroup(
                Chain(
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
                Chain(
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
                Chain(
                    InstanceAPIMethod(
                        Field, "setAccessible", s"([${AccessibleObject.toJVMTypeName}Z)V"
                    ),
                    InstanceAPIMethod(Field, "setAccessible", "(Z)V")
                ),
                "makes fields accessible"
            ),

            // setting methods or constructors accessible
            APIFeatureGroup(
                Chain(
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
                Chain(
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
                Chain(
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
