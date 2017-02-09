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
package util

import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.collection.immutable.Chain
import org.opalj.da.ClassFile

import scalafx.application.Platform
import scalafx.collections.ObservableBuffer

/**
 * A common feature extractor for simple API features. It supports in particular features that check
 * for certain API calls. More complex operations are not supported yet.
 *
 * Subclasses are only required to define a Chain of `APIFeatures`.
 *
 * Example of apiFeature declaration in a subclass:
 * {{{
 * val Unsafe = ObjectType("sun/misc/Unsafe")
 *
 * override def apiFeatures: Chain[APIFeatures] = Chain[APIFeature](
 * StaticAPIMethod(Unsafe, "getUnsafe", MethodDescriptor("()Lsun/misc/Unsafe;")),
 *
 * APIFeatureGroup(
 * Chain(
 * InstanceAPIMethod(Unsafe, "allocateInstance")
 * ), "Unsafe - Alloc"
 * ),
 *
 * APIFeatureGroup(
 * Chain(
 * InstanceAPIMethod(Unsafe, "arrayIndexScale"),
 * InstanceAPIMethod(Unsafe, "arrayBaseOffset")
 * ), "Unsafe - Array"
 * )
 * )
 * }}}
 *
 * @author Michael Reif
 */
trait APIFeatureExtractor extends FeatureExtractor {

    def apiFeatures: Chain[APIFeature]

    /**
     * The unique ids of the extracted features.
     */
    override def featureIDs: Seq[String] = apiFeatures.map(_.toFeatureID).toSeq

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

        var locations = Map.empty[String, ObservableBuffer[Location[S]]]

        for {
            cf ← project.allClassFiles
            m @ MethodWithBody(code) ← cf.methods
            apiFeature ← apiFeatures
            featureID = apiFeature.toFeatureID
            apiMethod ← apiFeature.getAPIMethods
            (pc, inst) ← code if inst.isInstanceOf[MethodInvocationInstruction]
            if !isInterrupted()
        } yield {
            var foundCall = false

            def putInstructionLocation(): Unit = {
                val source = project.source(cf)
                if (source.isEmpty)
                    return ;

                val cfLoc = ClassFileLocation(source.get, cf.fqn)
                val mSig = m.descriptor.toJava(m.name)
                val mLoc = MethodLocation(cfLoc, mSig)

                val instLoc = InstructionLocation(mLoc, pc)

                locations.get(featureID) match {
                    case None ⇒
                        val buf = ObservableBuffer[Location[S]](instLoc)
                        locations = locations + ((featureID, buf))
                    case Some(extensions) ⇒
                        locations = locations + ((featureID, extensions += instLoc))
                }
            }

            apiMethod match {
                case InstanceAPIMethod(declClass, name, None) ⇒
                    foundCall = inst match {
                        case INVOKEINTERFACE(`declClass`, `name`, _) ⇒
                            putInstructionLocation(); true
                        case INVOKEVIRTUAL(`declClass`, `name`, _) ⇒
                            putInstructionLocation(); true
                        case INVOKESPECIAL(`declClass`, _, `name`, _) ⇒
                            putInstructionLocation(); true
                        case _ ⇒ false
                    }
                case InstanceAPIMethod(declClass, name, Some(md)) ⇒
                    foundCall = inst match {
                        case INVOKEINTERFACE(`declClass`, `name`, `md`) ⇒
                            putInstructionLocation(); true
                        case INVOKEVIRTUAL(`declClass`, `name`, `md`) ⇒
                            putInstructionLocation(); true
                        case INVOKESPECIAL(`declClass`, _, `name`, `md`) ⇒
                            putInstructionLocation(); true
                        case _ ⇒ false
                    }
                case StaticAPIMethod(declClass, name, None) ⇒
                    foundCall = inst match {
                        case INVOKESTATIC(`declClass`, _, `name`, _) ⇒
                            putInstructionLocation(); true
                        case _ ⇒ false
                    }
                case StaticAPIMethod(declClass, name, Some(md)) ⇒
                    foundCall = inst match {
                        case INVOKESTATIC(`declClass`, _, `name`, `md`) ⇒
                            putInstructionLocation(); true
                        case _ ⇒ false
                    }
                case x ⇒ throw new UnknownError("Unsupported APIMethod type found: "+x.getClass)
            }

            if (foundCall && apiMethod.toFeatureID.contains("Unsafe")) {
                println(apiMethod.toFeatureID+" in "+m.toJava(cf))
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
                    val exts = locations.get(featureID)
                    if (exts.nonEmpty)
                        features(featureID).extensions ++= exts.get
                }
            }
        }
    }
}
