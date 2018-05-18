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

import org.opalj.collection.mutable.ArrayMap
import org.opalj.collection.immutable.Naught
import org.opalj.bi.Java10MajorVersion
import org.opalj.bi.Java5MajorVersion
import org.opalj.bi.Java1MajorVersion
import org.opalj.bi.jdkVersion
import org.opalj.br.analyses.Project

/**
 * Counts the number of class files per class file version.
 *
 * @author Michael Eichberg
 */
class ClassFileVersion(implicit hermes: HermesConfig) extends FeatureQuery {

    def featureId(majorVersion: Int) = s"Class File\n${jdkVersion(majorVersion)}"

    override val featureIDs: Seq[String] = {
        featureId(Java1MajorVersion) +: (
            for (majorVersion ← (Java5MajorVersion to Java10MajorVersion))
                yield featureId(majorVersion)
        )
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(da.ClassFile, S)]
    ): TraversableOnce[Feature[S]] = {

        val data = ArrayMap[LocationsContainer[S]](Java10MajorVersion)

        for {
            (classFile, source) ← project.projectClassFilesWithSources
            if !isInterrupted()
        } {
            val version = classFile.majorVersion
            val normalizedVersion = if (version < Java5MajorVersion) Java1MajorVersion else version
            var locations = data(normalizedVersion)
            if (locations eq null) {
                locations = new LocationsContainer[S]
                data(normalizedVersion) = locations
            }
            locations += ClassFileLocation[S](source, classFile)
        }

        {
            val java1MajorVersionFeatureId = this.featureId(Java1MajorVersion)
            val extensions = data(Java1MajorVersion)
            if (data(Java1MajorVersion) eq null)
                Feature[S](java1MajorVersionFeatureId, 0, Naught)
            else
                Feature[S](java1MajorVersionFeatureId, extensions.size, extensions)
        } +: (
            for (majorVersion ← (Java5MajorVersion to Java10MajorVersion)) yield {
                val featureId = this.featureId(majorVersion)
                val extensions = data(majorVersion)
                if (extensions ne null) {
                    Feature[S](featureId, extensions.size, extensions)
                } else
                    Feature[S](featureId, 0, Naught)
            }
        )
    }
}
