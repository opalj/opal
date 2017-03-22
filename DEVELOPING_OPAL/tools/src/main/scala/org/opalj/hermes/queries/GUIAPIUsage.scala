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
import org.opalj.da.CONSTANT_Utf8_info
import org.opalj.da.ClassFile

/**
 * Scans a class file's constant pool to check whether it refers to packages that belong to an API
 * for graphical user interfaces. The current analysis supports:
 *
 * - JavaFX (javafx.)
 * - SWT (org.eclipse.swt)
 * - Swing (javax.swing)
 * - AWT (java.awt)
 *
 * @author Michael Reif
 */
object GUIAPIUsage extends FeatureQuery {
    /**
     * The unique ids of the extracted features.
     */
    override def featureIDs: Seq[String] = Seq(
        /* 0 */ "JavaFX",
        /* 1 */ "SWT",
        /* 2 */ "Swing",
        /* 3 */ "AWT"
    )

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)]
    ): TraversableOnce[Feature[S]] = {

        val packageLocations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        for {
            (classFile, source) ← rawClassFiles
            cp = classFile.constant_pool
            cp_entry ← cp
        } {
            cp_entry match {
                case CONSTANT_Utf8_info(_, entry) ⇒
                    val location = ClassFileLocation(Some(source), classFile.thisType)
                    if (entry.startsWith("javafx.")) {
                        // note: package "javafx" is empty,
                        packageLocations(0) += location
                    } else if (entry.startsWith("org.eclipse.swt")) {
                        packageLocations(1) += location
                    } else if (entry.startsWith(("javax.swing"))) {
                        packageLocations(2) += location
                    } else if (entry.startsWith(("java.awt"))) {
                        packageLocations(3) += location
                    }

                case _ ⇒
            }
        }

        for { (featureID, featureIDIndex) ← featureIDs.iterator.zipWithIndex } yield {
            Feature[S](featureID, packageLocations(featureIDIndex))
        }
    }
}
