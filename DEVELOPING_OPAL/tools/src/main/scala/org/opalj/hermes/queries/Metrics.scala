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

import scala.collection.mutable
import org.opalj.br.analyses.Project
import org.opalj.br.cfg.CFGFactory

/**
 * Extracts basic metric information (Fields/Methods per Class; Classes per Package; etc.).
 *
 * @author Michael Reif
 */
class Metrics(implicit hermes: HermesConfig) extends FeatureQuery {

    /**
     * The unique ids of the extracted features.
     */
    override val featureIDs: Seq[String] = {
        Seq(
            "0 FPC", "1-3 FPC", "4-10 FPC", ">10 FPC", // 0, 1, 2, 3
            "0 MPC", "1-3 MPC", "4-10 MPC", ">10 MPC", // 4, 5, 6, 7
            "1-3 CPP", "4-10 CPP", ">10 CPP", // 8, 9, 10
            "0 NOC", "1-3 NOC", "4-10 NOC", ">10 NOC", //  11, 12, 13, 14
            "linear methods (McCabe)", "2-3 McCabe", "4-10 McCabe", ">10 McCabe" // 15, 16, 17 ,18
        )
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(da.ClassFile, S)]
    ): TraversableOnce[Feature[S]] = {

        val classLocations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        class PackageInfo(var classesCount: Int = 0, val location: PackageLocation[S])
        val packagesInfo = mutable.Map.empty[String, PackageInfo]

        val classHierarchy = project.classHierarchy

        for {
            (classFile, source) ← project.projectClassFilesWithSources
            classLocation = ClassFileLocation(source, classFile)
        } {
            // fpc

            classFile.fields.size match {
                case 0            ⇒ classLocations(0) += classLocation
                case x if x <= 3  ⇒ classLocations(1) += classLocation
                case x if x <= 10 ⇒ classLocations(2) += classLocation
                case x            ⇒ classLocations(3) += classLocation
            }

            // mpc

            classFile.methods.size match {
                case 0            ⇒ classLocations(4) += classLocation
                case x if x <= 3  ⇒ classLocations(5) += classLocation
                case x if x <= 10 ⇒ classLocations(6) += classLocation
                case x            ⇒ classLocations(7) += classLocation
            }

            // noc

            classHierarchy.directSubtypesOf(classFile.thisType).size match {
                case 0            ⇒ classLocations(11) += classLocation
                case x if x <= 3  ⇒ classLocations(12) += classLocation
                case x if x <= 10 ⇒ classLocations(13) += classLocation
                case x            ⇒ classLocations(14) += classLocation
            }

            // count the classes per package
            val packageName = classFile.thisType.packageName
            val packageInfo = packagesInfo.getOrElseUpdate(
                packageName,
                new PackageInfo(location = PackageLocation(packageName))
            )
            packageInfo.classesCount += 1

            // McCabe
            classFile.methods foreach { method ⇒
                CFGFactory(method, project.classHierarchy) foreach { cfg ⇒
                    val methodLocation = MethodLocation(classLocation, method)
                    val bbs = cfg.reachableBBs
                    val edges = bbs.foldLeft(0) { (res, node) ⇒
                        res + node.successors.size
                    }
                    val mcCabe = edges - bbs.size + 2
                    mcCabe match {
                        case 1            ⇒ classLocations(15) += methodLocation
                        case x if x <= 3  ⇒ classLocations(16) += methodLocation
                        case x if x <= 10 ⇒ classLocations(17) += methodLocation
                        case x            ⇒ classLocations(18) += methodLocation
                    }
                }
            }
        }

        packagesInfo.values foreach { pi ⇒
            pi.classesCount match {
                case x if x <= 3  ⇒ classLocations(8) += pi.location
                case x if x <= 10 ⇒ classLocations(9) += pi.location
                case x            ⇒ classLocations(10) += pi.location
            }
        }

        for { (featureID, featureIDIndex) ← featureIDs.iterator.zipWithIndex } yield {
            Feature[S](featureID, classLocations(featureIDIndex))
        }
    }
}
