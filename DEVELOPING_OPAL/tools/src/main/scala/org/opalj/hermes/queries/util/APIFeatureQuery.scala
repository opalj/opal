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

import scala.collection.mutable
import org.opalj.collection.immutable.Naught
import org.opalj.collection.immutable.Chain
import org.opalj.br.MethodWithBody
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.da.ClassFile

/**
 * A predefined query for finding simple API features. It supports - in particular -
 * features that check for certain API calls. Subclasses are only required to define
 * a `Chain` of `APIFeatures`.
 *
 * Example of an `apiFeature` declaration in a subclass:
 * {{{
 * val Unsafe = ObjectType("sun/misc/Unsafe")
 *
 * override def apiFeatures: Chain[APIFeatures] = Chain[APIFeature](
 *  StaticAPIMethod(Unsafe, "getUnsafe", MethodDescriptor("()Lsun/misc/Unsafe;")),
 *  APIFeatureGroup(
 *      Chain(
 *          InstanceAPIMethod(Unsafe, "allocateInstance")
 *      ), "Unsafe - Alloc"
 *  ),
 *  APIFeatureGroup(
 *      Chain(
 *          InstanceAPIMethod(Unsafe, "arrayIndexScale"),
 *          InstanceAPIMethod(Unsafe, "arrayBaseOffset")
 *      ), "Unsafe - Array"
 *  )
 * )
 * }}}
 *
 * @author Michael Reif
 */
trait APIFeatureQuery extends FeatureQuery {

    def apiFeatures: Chain[APIFeature]

    /*
     * Returns a set of all relevant receiver types.
     */
    private[this] def usedAPITypes: Set[ObjectType] = {
        apiFeatures.foldLeft(Set.empty[ObjectType]) { (res, acc) ⇒
            res ++ acc.getAPIMethods.map(_.declClass)
        }
    }

    /**
     * The unique ids of the computed features.
     */
    override def featureIDs: Seq[String] = apiFeatures.map(_.toFeatureID).toSeq

    /**
     * The function which analyzes the project and extracts the feature information.
     *
     * @note '''Every query should regularly check that its thread is not interrupted!''' E.g.,
     *       using `Thread.currentThread().isInterrupted()`.
     */
    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)]
    ): TraversableOnce[Feature[S]] = {

        val apiTypes = usedAPITypes

        var occurrencesCount = apiFeatures.foldLeft(Map.empty[String, Int])(
            (result, feature) ⇒ result + ((feature.toFeatureID, 0))
        )

        val locations = mutable.Map.empty[String, Chain[Location[S]]]

        val classFeatures = apiFeatures.collect { case ce: ClassExtension ⇒ ce }

        val classHierarchy = project.classHierarchy
        for {
            classFeature ← classFeatures
            featureID = classFeature.toFeatureID
        } yield {
            val subtypes = classHierarchy.allSubtypes(classFeature.declClass, reflexive = false).
                filter(project.isProjectType)
            val size = subtypes.size

            val count = occurrencesCount.get(featureID).get + size
            occurrencesCount = occurrencesCount + ((featureID, count))

            if (size > 0) {
                subtypes.filter(project.isProjectType).foreach { ot ⇒
                    val cfLoc = getClassFileLocation(project, ot)
                    if (cfLoc.nonEmpty)
                        locations += ((
                            featureID,
                            cfLoc.get :&: locations.getOrElse(featureID, Naught)
                        ))
                }
            }
        }

        // Checking method API features

        for {
            cf ← project.allProjectClassFiles
            if !isInterrupted()
            m @ MethodWithBody(code) ← cf.methods
            (pc, mii @ MethodInvocationInstruction(declClass, _, name, methodDescriptor)) ← code
            if declClass.isObjectType && apiTypes.contains(declClass.asObjectType)
            apiFeature ← apiFeatures
            featureID = apiFeature.toFeatureID
            apiMethod ← apiFeature.getAPIMethods
        } yield {
            def putInstructionLocation(): Unit = {
                val source = project.source(cf)
                if (source.isEmpty)
                    return ;

                val instLoc = InstructionLocation(source.get, cf, m, pc)
                locations += ((featureID, instLoc :&: locations.getOrElse(featureID, Naught)))

                val count = occurrencesCount.get(featureID).get + 1
                occurrencesCount = occurrencesCount + ((featureID, count))
            }

            apiMethod match {
                case InstanceAPIMethod(
                    `declClass`,
                    `name`,
                    None | Some(`methodDescriptor`)
                    ) if mii.isInstanceMethod ⇒
                    putInstructionLocation()
                case StaticAPIMethod(
                    `declClass`,
                    `name`,
                    None | Some(`methodDescriptor`)
                    ) if !mii.isInstanceMethod ⇒
                    putInstructionLocation()
                case _ ⇒ /*empty*/
            }
        }

        apiFeatures.map { apiFeature ⇒
            val featureID = apiFeature.toFeatureID
            Feature(
                featureID,
                occurrencesCount.get(featureID).get,
                locations.getOrElse(featureID, Naught)
            )
        }
    }

    private[this] def getClassFileLocation[S](
        project:    Project[S],
        objectType: ObjectType
    ): Option[ClassFileLocation[S]] = {
        val classFile = project.classFile(objectType)
        classFile match {
            case Some(classFile) ⇒ project.source(classFile) match {
                case Some(source) ⇒ Some(ClassFileLocation(source, classFile))
                case None         ⇒ None
            }
            case None ⇒ None
        }
    }
}
