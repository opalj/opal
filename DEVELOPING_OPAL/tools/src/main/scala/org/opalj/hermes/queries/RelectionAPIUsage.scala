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
import org.opalj.hermes.queries.util.APIFeatureExtractor
import org.opalj.hermes.queries.util.APIFeatureGroup
import org.opalj.hermes.queries.util.InstanceAPIMethod

/**
 * Counts the number of certain calls to the Java Reflection API.
 *
 * @author Michael Reif
 */
object ReflectionAPIUsage extends APIFeatureExtractor {

    val ClassOt = ObjectType("java/lang/Class")
    val FieldOt = ObjectType("java/lang/Field")
    val AccessibleObjectOt = ObjectType("java/lang/reflect/AccessibleObject")
    val ConstructorOt = ObjectType("java/lang/reflect/Constructor")
    val MethodOt = ObjectType("java/lang/reflect/Method")

    def apiFeatures: Chain[APIFeature] = Chain[APIFeature](
        InstanceAPIMethod(ClassOt, "forName"),

        // reflective instance creation
        APIFeatureGroup(
            Chain(
                InstanceAPIMethod(ClassOt, "newInstance", MethodDescriptor.JustReturnsObject),
                InstanceAPIMethod(ConstructorOt, "newInstance", MethodDescriptor("([Ljava/lang/Object;)Ljava/lang/Object;"))
            ), "reflective instance creation"
        ),

        // reflective field write api
        APIFeatureGroup(
            Chain(
                InstanceAPIMethod(FieldOt, "set", MethodDescriptor("(Ljava/lang/Object;Ljava/lang/Object;)V")),
                InstanceAPIMethod(FieldOt, "setBoolean", MethodDescriptor("(Ljava/lang/Object;Z)V")),
                InstanceAPIMethod(FieldOt, "setByte", MethodDescriptor("(Ljava/lang/Object;B)V")),
                InstanceAPIMethod(FieldOt, "setChar", MethodDescriptor("(Ljava/lang/Object;C)V")),
                InstanceAPIMethod(FieldOt, "setDouble", MethodDescriptor("(Ljava/lang/Object;D)V")),
                InstanceAPIMethod(FieldOt, "setFloat", MethodDescriptor("(Ljava/lang/Object;F)V")),
                InstanceAPIMethod(FieldOt, "setInt", MethodDescriptor("(Ljava/lang/Object;I)V")),
                InstanceAPIMethod(FieldOt, "setLong", MethodDescriptor("(Ljava/lang/Object;J)V")),
                InstanceAPIMethod(FieldOt, "setShort", MethodDescriptor("(Ljava/lang/Object;S)V"))
            ), "reflective field write"
        ),

        // reflective field read api
        APIFeatureGroup(
            Chain(
                InstanceAPIMethod(FieldOt, "get", MethodDescriptor("(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")),
                InstanceAPIMethod(FieldOt, "getBoolean", MethodDescriptor("(Ljava/lang/Object;)Z")),
                InstanceAPIMethod(FieldOt, "getByte", MethodDescriptor("(Ljava/lang/Object;)B")),
                InstanceAPIMethod(FieldOt, "getChar", MethodDescriptor("(Ljava/lang/Object;)C")),
                InstanceAPIMethod(FieldOt, "getDouble", MethodDescriptor("(Ljava/lang/Object;)D")),
                InstanceAPIMethod(FieldOt, "getFloat", MethodDescriptor("(Ljava/lang/Object;)F")),
                InstanceAPIMethod(FieldOt, "getInt", MethodDescriptor("(Ljava/lang/Object;)I")),
                InstanceAPIMethod(FieldOt, "getLong", MethodDescriptor("(Ljava/lang/Object;)J")),
                InstanceAPIMethod(FieldOt, "getShort", MethodDescriptor("(Ljava/lang/Object;)S"))
            ), "reflective field read"
        ),

        // setting fields accessible
        APIFeatureGroup(
            Chain(
                InstanceAPIMethod(
                    FieldOt,
                    "setAccessible",
                    MethodDescriptor(s"([${AccessibleObjectOt.toJVMTypeName}Z)V")
                ),
                InstanceAPIMethod(FieldOt, "setAccessible", MethodDescriptor("(Z)V"))
            ), "set fields accessible"
        ),

        // setting methods or constructors accessible
        APIFeatureGroup(
            Chain(
                InstanceAPIMethod(
                    MethodOt,
                    "setAccessible",
                    MethodDescriptor(s"([${AccessibleObjectOt.toJVMTypeName}Z)V")
                ),
                InstanceAPIMethod(MethodOt, "setAccessible", MethodDescriptor("(Z)V")),
                InstanceAPIMethod(
                    ConstructorOt,
                    "setAccessible",
                    MethodDescriptor(s"([${AccessibleObjectOt.toJVMTypeName}Z)V")
                ),
                InstanceAPIMethod(ConstructorOt, "setAccessible", MethodDescriptor("(Z)V"))
            ), "set methods or constructors accessible"
        ),

        // set an AccessibleObject accessible
        APIFeatureGroup(
            Chain(
                InstanceAPIMethod(
                    AccessibleObjectOt,
                    "setAccessible",
                    MethodDescriptor(s"([${AccessibleObjectOt.toJVMTypeName}Z)V")
                ),
                InstanceAPIMethod(AccessibleObjectOt, "setAccessible", MethodDescriptor("(Z)V"))
            ), "set an AccessibleObject accessible (exact type unknown)"
        ),

        // reflective method invocation
        InstanceAPIMethod(
            MethodOt,
            "invoke",
            MethodDescriptor(s"(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;")
        )
    )
}