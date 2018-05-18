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
 * override def apiFeatures: Chain[APIFeatures] = Chain[APIFeature](
 *  val Unsafe = ObjectType("sun/misc/Unsafe")
 *
 *  StaticAPIMethod(Unsafe, "getUnsafe", MethodDescriptor("()Lsun/misc/Unsafe;")),
 *  APIFeatureGroup(
 *      Chain(InstanceAPIMethod(Unsafe, "allocateInstance")),
 *      "Unsafe - Alloc"
 *  ),
 *  APIFeatureGroup(
 *      Chain(
 *          InstanceAPIMethod(Unsafe, "arrayIndexScale"),
 *          InstanceAPIMethod(Unsafe, "arrayBaseOffset")
 *      ),
 *      "Unsafe - Array"
 *  )
 * )
 * }}}
 *
 * @author Michael Reif
 */
abstract class APIFeatureQuery(implicit hermes: HermesConfig) extends FeatureQuery {

    def apiFeatures: Chain[APIFeature]

    /**
     * The unique ids of the computed features.
     */
    override lazy val featureIDs: Seq[String] = apiFeatures.map(_.featureID).toSeq

    /**
     * Returns the set of all relevant receiver types.
     */
    private[this] final lazy val apiTypes: Set[ObjectType] = {
        apiFeatures.foldLeft(Set.empty[ObjectType])(_ ++ _.apiMethods.map(_.declClass))
    }

    /**
     * Analyzes the project and extracts the feature information.
     *
     * @note '''Every query should regularly check that its thread is not interrupted!''' E.g.,
     *       using `Thread.currentThread().isInterrupted()`.
     */
    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)]
    ): TraversableOnce[Feature[S]] = {

        val classHierarchy = project.classHierarchy
        import classHierarchy.allSubtypes
        import project.isProjectType

        def getClassFileLocation(objectType: ObjectType): Option[ClassFileLocation[S]] = {
            val classFile = project.classFile(objectType)
            classFile.flatMap { cf ⇒ project.source(cf).map(src ⇒ ClassFileLocation(src, cf)) }
        }

        var occurrencesCount = apiFeatures.foldLeft(Map.empty[String, Int])(
            (result, feature) ⇒ result + ((feature.featureID, 0))
        )

        // TODO Use LocationsContainer
        val locations = mutable.Map.empty[String, Chain[Location[S]]]

        for {
            classFeature ← apiFeatures.collect { case ce: ClassExtension ⇒ ce }
            featureID = classFeature.featureID
            subtypes = allSubtypes(classFeature.declClass, reflexive = false).filter(isProjectType)
            size = subtypes.size
            if size > 0
        } {
            val count = occurrencesCount(featureID) + size
            occurrencesCount += ((featureID, count))

            for {
                subtype ← subtypes
                if project.isProjectType(subtype)
                classFileLocation ← getClassFileLocation(subtype)
            } {
                locations += ((
                    featureID,
                    classFileLocation :&: locations.getOrElse(featureID, Naught)
                ))
            }
        }

        // Checking method API features

        for {
            cf ← project.allProjectClassFiles
            if !isInterrupted()
            source ← project.source(cf)
            m @ MethodWithBody(code) ← cf.methods
            pcAndInvocation ← code collect { case mii: MethodInvocationInstruction ⇒ mii }
            pc = pcAndInvocation.pc
            mii = pcAndInvocation.value
            declClass = mii.declaringClass
            if declClass.isObjectType
            if apiTypes.contains(declClass.asObjectType)
            apiFeature ← apiFeatures
            featureID = apiFeature.featureID
            APIMethod ← apiFeature.apiMethods
            if APIMethod.matches(mii)
        } {
            val l = InstructionLocation(source, m, pc)
            locations += ((featureID, l :&: locations.getOrElse(featureID, Naught)))
            val count = occurrencesCount(featureID) + 1
            occurrencesCount = occurrencesCount + ((featureID, count))
        }

        apiFeatures.map { apiFeature ⇒
            val featureID = apiFeature.featureID
            Feature(
                featureID,
                occurrencesCount(featureID),
                locations.getOrElse(featureID, Naught)
            )
        }
    }

}
