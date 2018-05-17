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

import org.opalj.br.analyses.Project
import org.opalj.da.ClassFile
import org.opalj.hermes.{Feature, FeatureQuery, ProjectConfiguration}

/**
 * Computes the size of the inheritance tree for each class of a project and then assigns the
 * class to its respective category.
 *
 * Here, the size is measured by counting all super types (interfaces and classes!)
 *
 * @author Ben Hermann
 * @author Michael Eichberg
 */
object SizeOfInheritanceTree extends FeatureQuery {

    override val featureIDs: List[String] = {
        List(
            /*0*/ "Very Small Inheritance Tree", // [0 ... 1 x CategorySize)
            /*1*/ "Small Inheritance Tree", // [1 x CategorySize ... 2 x CategorySize)
            /*2*/ "Medium Inheritance Tree", // [2 x CategorySize ... 3 x CategorySize)
            /*3*/ "Large Inheritance Tree", // [3 x CategorySize ... 4 x CategorySize)
            /*4*/ "Very Large Inheritance Tree", // [4 x CategorySize ... 5 x CategorySize)
            /*5*/ "Huge Inheritance Tree", // [5 x CategorySize ... 6 x Int.MaxValue)

            /*6*/ "Size of the Inheritance Tree Unknown" // The project is not complete
        )
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)]
    ): TraversableOnce[Feature[S]] = {
        val classHierarchy = project.classHierarchy
        import classHierarchy.isSupertypeInformationComplete

        /* Determines the size for each category. */
        val CategorySize: Int = 3 // TODO read from project config file

        val features = Array.fill[LocationsContainer[S]](featureIDs.size)(new LocationsContainer[S])
        var classCount = 0
        var sumOfSizeOfInheritanceTrees = 0
        val supertypes = classHierarchy.supertypeInformation
        classHierarchy.foreachKnownType { t ⇒
            if (project.isProjectType(t)) {
                val l = ClassFileLocation(project, t)
                supertypes.get(t) match {
                    case Some(supertypeInformation) if isSupertypeInformationComplete(t) ⇒
                        val sizeOfInheritanceTree = supertypeInformation.size
                        features(Math.min(sizeOfInheritanceTree / CategorySize, 5)) += l
                        classCount += 1
                        sumOfSizeOfInheritanceTrees += sizeOfInheritanceTree
                    case _ /* None or <incomplete> */ ⇒
                        features(6) += l
                }
            }
        }

        projectConfiguration.addStatistic(
            "⟨SizeOfInheritanceTree⟩",
            if (classCount != 0) {
                sumOfSizeOfInheritanceTrees.toDouble / classCount.toDouble
            } else {
                0D
            }
        )

        for { (featureID, featureIDIndex) ← featureIDs.iterator.zipWithIndex } yield {
            Feature[S](featureID, features(featureIDIndex))
        }

    }

}
