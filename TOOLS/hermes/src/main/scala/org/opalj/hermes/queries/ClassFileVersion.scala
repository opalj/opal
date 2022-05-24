/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import org.opalj.collection.mutable.ArrayMap
import org.opalj.bi.LatestSupportedJavaMajorVersion
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

    def featureId(majorVersion: Int) = s"${jdkVersion(majorVersion)} Class File"

    override val featureIDs: Seq[String] = {
        featureId(Java1MajorVersion) +: (
            for (majorVersion <- Java5MajorVersion to LatestSupportedJavaMajorVersion)
                yield featureId(majorVersion)
        )
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(da.ClassFile, S)]
    ): IterableOnce[Feature[S]] = {

        val data = ArrayMap[LocationsContainer[S]](LatestSupportedJavaMajorVersion)

        for {
            (classFile, source) <- project.projectClassFilesWithSources
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
                Feature[S](java1MajorVersionFeatureId, 0, List.empty)
            else
                Feature[S](java1MajorVersionFeatureId, extensions.size, extensions)
        } +: (
            for (majorVersion <- Java5MajorVersion to LatestSupportedJavaMajorVersion) yield {
                val featureId = this.featureId(majorVersion)
                val extensions = data(majorVersion)
                if (extensions ne null) {
                    Feature[S](featureId, extensions.size, extensions)
                } else
                    Feature[S](featureId, 0, List.empty)
            }
        )
    }
}
