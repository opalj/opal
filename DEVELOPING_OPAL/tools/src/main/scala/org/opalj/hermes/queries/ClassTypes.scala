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
 * Counts which kinds of class types are actually defined.
 *
 * @author Michael Eichberg
 */
object ClassTypes extends FeatureQuery {

    override def featureIDs: List[String] = {
        List(
            /*0*/ "(concrete) classes",
            /*1*/ "abstract classes",
            /*2*/ "annotations",
            /*3*/ "enumerations",
            /*4*/ "marker interfaces",
            /*5*/ "functional interfaces\n(single abstract method (SAM) interface)",
            /*6*/ "interface\nwith default methods (Java >8)",
            /*7*/ "interface\nwith static methods (Java >8)",
            /*8*/ "module (Java >9)"
        )
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(da.ClassFile, S)]
    ): TraversableOnce[Feature[S]] = {

        val classTypesLocations = Array.fill(9)(new LocationsContainer[S])

        for {
            (classFile, source) ← project.projectClassFilesWithSources
        } {
            val location = ClassFileLocation(source, classFile)

            if (classFile.isClassDeclaration) {
                if (!classFile.isAbstract) {
                    classTypesLocations(0) += location
                } else {
                    classTypesLocations(1) += location
                }
            } else if (classFile.isAnnotationDeclaration) {
                classTypesLocations(2) += location
            } else if (classFile.isEnumDeclaration) {
                classTypesLocations(3) += location
            } else if (classFile.isInterfaceDeclaration) {
                if (classFile.methods.length == 0) {
                    classTypesLocations(4) += location
                } else if (classFile.methods.length == 1 && classFile.methods(0).isAbstract) {
                    classTypesLocations(5) += location
                } else {
                    if (classFile.methods.exists(m ⇒ !m.isAbstract && !m.isStatic))
                        classTypesLocations(6) += location
                    if (classFile.methods.exists(m ⇒ m.isStatic && m.name != "<clinit>"))
                        classTypesLocations(7) += location
                }
            } else if (classFile.isModuleDeclaration) {
                classTypesLocations(8) += location
            }
        }

        for { (featureID, featureIDIndex) ← featureIDs.zipWithIndex } yield {
            Feature[S](featureID, classTypesLocations(featureIDIndex))
        }
    }
}
