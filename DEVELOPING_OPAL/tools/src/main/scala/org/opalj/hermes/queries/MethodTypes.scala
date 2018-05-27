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

/**
 * Counts which types of methods types are found.
 *
 * @author Michael Eichberg
 */
class MethodTypes(implicit hermes: HermesConfig) extends FeatureQuery {

    override val featureIDs: List[String] = {
        List(
            /*0*/ "native methods",
            /*1*/ "synthetic methods",
            /*2*/ "bridge methods",
            /*3*/ "synchronized methods",
            /*4*/ "varargs methods",
            // second category...
            /*5*/ "static initializers",
            /*6*/ "static methods\n(not including static initializers)",
            /*7*/ "constructors",
            /*8*/ "instance methods"
        )
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(da.ClassFile, S)]
    ): TraversableOnce[Feature[S]] = {

        val methodLocations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        for {
            (classFile, source) ← project.projectClassFilesWithSources
            if !isInterrupted()
            classLocation = ClassFileLocation(source, classFile)
            m ← classFile.methods
        } {
            val location = MethodLocation(classLocation, m)
            if (m.isNative) methodLocations(0) += location
            if (m.isSynthetic) methodLocations(1) += location
            if (m.isBridge) methodLocations(2) += location
            if (m.isSynchronized) methodLocations(3) += location
            if (m.isVarargs) methodLocations(4) += location

            if (m.name == "<clinit>")
                methodLocations(5) += location
            else {
                if (m.isStatic) {
                    methodLocations(6) += location
                } else {
                    if (m.name == "<init>")
                        methodLocations(7) += location
                    else
                        methodLocations(8) += location
                }
            }
        }

        for { (featureID, featureIDIndex) ← featureIDs.iterator.zipWithIndex } yield {
            Feature[S](featureID, methodLocations(featureIDIndex))
        }
    }
}
