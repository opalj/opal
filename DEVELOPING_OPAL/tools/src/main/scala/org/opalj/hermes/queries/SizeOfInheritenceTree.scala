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
package org.opalj.hermes.queries

import org.opalj.br.analyses.Project
import org.opalj.da.ClassFile
import org.opalj.hermes.{Feature, FeatureQuery, ProjectConfiguration}

/**
 * Computes the depth of inheritance tree for each class of a project and then assigns the
 * class to its respective category.
 *
 * Here, the depth is measured using the shortest path from `java.lang.Object` to the respective
 * class.
 *
 * @author Ben Hermann
 */
object InheritenceTreeDepth extends FeatureQuery {

    override val featureIDs: List[String] = {
        List(
            "Very Small", // [0 ... 1 x CategorySize)
            "Small", // [CategorySize ... 2 x CategorySize)
            "Medium", // [CategorySize ... 3 x CategorySize)
            "High", // [CategorySize ... 4 x CategorySize)
            "Very High", // [CategorySize ... 5 x CategorySize)
            "Extremely High", // [CategorySize ... 6 x Int.MaxValue)
            "Unknown" // The project is not complete
        )
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)]
    ): TraversableOnce[Feature[S]] = {

        /* Determins the size for each category. */
        val CategorySize: Int = 3 // TODO read from config file

        val features = Array.fill[Int](featureIDs.length)(0)

        val hierarchySizes = project.classHierarchy.leafClassTypes.map(c ⇒ project.classHierarchy.allSubtypes(c, false).size)

        features(0) = hierarchySizes.min
        features(1) = hierarchySizes.max

        null
        /*for { (featureID, featureIDIndex) ← featureIDs.iterator.zipWithIndex } yield {
        Feature[S](featureID, features(featureIDIndex))
    }*/

    }

}
