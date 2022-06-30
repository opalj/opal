/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import org.opalj.br.analyses.Project
import org.opalj.da.ClassFile

import scala.collection.immutable.ArraySeq

/**
 * Testcase to find applications containing native methods.
 *
 * @author Dominik Helm
 */
class NativeMethods(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    override def featureIDs: Seq[String] = Seq("NM")

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {

        val locations = new LocationsContainer[S]

        for {
            (classFile, source) <- project.projectClassFilesWithSources
            method <- classFile.methods
            if method.isNative
        } {
            locations += MethodLocation(source, method)
        }

        ArraySeq(locations)
    }
}