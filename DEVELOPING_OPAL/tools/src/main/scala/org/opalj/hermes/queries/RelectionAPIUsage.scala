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
import org.opalj.br.MethodWithBody
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.collection.immutable.Chain
import org.opalj.da.ClassFile
import org.opalj.hermes.queries.util.APIFeature
import org.opalj.hermes.queries.util.APIFeatureGroup
import org.opalj.hermes.queries.util.APIMethod

import scalafx.application.Platform

/**
 * Counts the number of certain calls to the Java Reflection API.
 *
 * @author Michael Reif
 */
object ReflectionAPIUsage extends FeatureExtractor {

    val ClassOt = ObjectType("java/lang/Class")
    val FieldOt = ObjectType("java/lang/Field")
    val AccessibleObjectOt = ObjectType("java/lang/reflect/AccessibleObject")
    val ConstructorOt = ObjectType("java/lang/reflect/Constructor")
    val MethodOt = ObjectType("java/lang/reflect/Method")

    val apiFeatures = List[APIFeature](
        APIMethod(ClassOt, "forName", MethodDescriptor(ObjectType.String, ClassOt), isStatic = true),
        APIMethod(
            ClassOt,
            "forName",
            MethodDescriptor(s"(Ljava/lang/String;ZLjava/lang/ClassLoader;)[Ljava/lang/Class;"),
            isStatic = true
        ),

        // reflective instance creation
        APIFeatureGroup(
            Chain(
                APIMethod(ClassOt, "newInstance", MethodDescriptor.JustReturnsObject),
                APIMethod(ConstructorOt, "newInstance", MethodDescriptor("([Ljava/lang/Object;)Ljava/lang/Object;"))
            ), "reflective instance creation"
        ),

        // reflective field write api
        APIFeatureGroup(
            Chain(
                APIMethod(FieldOt, "set", MethodDescriptor("(Ljava/lang/Object;Ljava/lang/Object;)V")),
                APIMethod(FieldOt, "setBoolean", MethodDescriptor("(Ljava/lang/Object;Z)V")),
                APIMethod(FieldOt, "setByte", MethodDescriptor("(Ljava/lang/Object;B)V")),
                APIMethod(FieldOt, "setChar", MethodDescriptor("(Ljava/lang/Object;C)V")),
                APIMethod(FieldOt, "setDouble", MethodDescriptor("(Ljava/lang/Object;D)V")),
                APIMethod(FieldOt, "setFloat", MethodDescriptor("(Ljava/lang/Object;F)V")),
                APIMethod(FieldOt, "setInt", MethodDescriptor("(Ljava/lang/Object;I)V")),
                APIMethod(FieldOt, "setLong", MethodDescriptor("(Ljava/lang/Object;J)V")),
                APIMethod(FieldOt, "setShort", MethodDescriptor("(Ljava/lang/Object;S)V"))
            ), "reflective field write"
        ),

        // reflective field read api
        APIFeatureGroup(
            Chain(
                APIMethod(FieldOt, "get", MethodDescriptor("(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")),
                APIMethod(FieldOt, "getBoolean", MethodDescriptor("(Ljava/lang/Object;)Z")),
                APIMethod(FieldOt, "getByte", MethodDescriptor("(Ljava/lang/Object;)B")),
                APIMethod(FieldOt, "getChar", MethodDescriptor("(Ljava/lang/Object;)C")),
                APIMethod(FieldOt, "getDouble", MethodDescriptor("(Ljava/lang/Object;)D")),
                APIMethod(FieldOt, "getFloat", MethodDescriptor("(Ljava/lang/Object;)F")),
                APIMethod(FieldOt, "getInt", MethodDescriptor("(Ljava/lang/Object;)I")),
                APIMethod(FieldOt, "getLong", MethodDescriptor("(Ljava/lang/Object;)J")),
                APIMethod(FieldOt, "getShort", MethodDescriptor("(Ljava/lang/Object;)S"))
            ), "reflective field read"
        ),

        // setting fields accessible
        APIFeatureGroup(
            Chain(
                APIMethod(
                    FieldOt,
                    "setAccessible",
                    MethodDescriptor(s"([${AccessibleObjectOt.toJVMTypeName}Z)V"),
                    isStatic = true
                ),
                APIMethod(FieldOt, "setAccessible", MethodDescriptor("(Z)V"))
            ), "set fields accessible"
        ),

        // setting methods or constructors accessible
        APIFeatureGroup(
            Chain(
                APIMethod(
                    MethodOt,
                    "setAccessible",
                    MethodDescriptor(s"([${AccessibleObjectOt.toJVMTypeName}Z)V"),
                    isStatic = true
                ),
                APIMethod(MethodOt, "setAccessible", MethodDescriptor("(Z)V")),
                APIMethod(
                    ConstructorOt,
                    "setAccessible",
                    MethodDescriptor(s"([${AccessibleObjectOt.toJVMTypeName}Z)V"),
                    isStatic = true
                ),
                APIMethod(ConstructorOt, "setAccessible", MethodDescriptor("(Z)V"))
            ), "set methods or constructors accessible"
        ),

        // set an AccessibleObject accessible
        APIFeatureGroup(
            Chain(
                APIMethod(
                    AccessibleObjectOt,
                    "setAccessible",
                    MethodDescriptor(s"([${AccessibleObjectOt.toJVMTypeName}Z)V"),
                    isStatic = true
                ),
                APIMethod(AccessibleObjectOt, "setAccessible", MethodDescriptor("(Z)V"))
            ), "set an AccessibleObject accessible (exact type unknown)"
        ),

        // reflective method invocation
        APIMethod(
            MethodOt,
            "invoke",
            MethodDescriptor(s"(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;")
        )
    )

    /**
     * The unique ids of the extracted features.
     */
    override def featureIDs: Seq[String] = apiFeatures.map(_.toFeatureID)

    /**
     * The major function which analyzes the project and extracts the feature information.
     *
     * @note '''Every query should regularly check that its thread is not interrupted!''' E.g.,
     *       using `Thread.currentThread().isInterrupted()`.
     */
    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)],
        features:             Map[String, Feature[S]]
    ): Unit = {

        var invocationCounts = apiFeatures.foldLeft(Map.empty[String, Int])(
            (result, feature) ⇒ result + ((feature.toFeatureID, 0))
        )

        //        println(invocationCounts.keySet.mkString("keyset:[", "\n,\n", "]"))

        for {
            cf ← project.allClassFiles
            MethodWithBody(code) ← cf.methods
            apiFeature ← apiFeatures
            featureID = apiFeature.toFeatureID
            APIMethod(declClass, name, descriptor, isStatic) ← apiFeature.getAPIMethods
            inst ← code.instructions if inst.isInstanceOf[MethodInvocationInstruction]
            if !isInterrupted()
        } yield {

            val foundCall = inst match {
                case INVOKESTATIC(`declClass`, _, `name`, `descriptor`) if isStatic ⇒ true
                case INVOKEVIRTUAL(`declClass`, `name`, `descriptor`) if !isStatic ⇒ true
                case INVOKESPECIAL(`declClass`, _, `name`, `descriptor`) if !isStatic ⇒ true
                case _ ⇒ false
            }

            if (foundCall) {
                val count = invocationCounts.get(featureID).get + 1
                invocationCounts = invocationCounts + ((featureID, count))
            }
        }

        if (!isInterrupted) { // we want to avoid that we return partial results
            Platform.runLater {
                apiFeatures.foreach { apiMethod ⇒
                    val featureID = apiMethod.toFeatureID
                    features(featureID).count.value = invocationCounts.get(featureID).get
                }
            }
        }
    }
}