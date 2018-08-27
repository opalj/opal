/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.Project
import org.opalj.da.ClassFile

/**
 * Test case that finds static methods on interfaces.
 *
 * @author Dominik Helm
 */
class StaticInterfaceMethods(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    override def featureIDs: Seq[String] = {
        Seq(
            "SIF"
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {

        val instructionsLocations = new LocationsContainer[S]

        for {
            (classFile, source) ← project.projectClassFilesWithSources
            if !isInterrupted()
            if classFile.isInterfaceDeclaration
            classFileLocation = ClassFileLocation(source, classFile)
            method @ MethodWithBody(body) ← classFile.methods
            if method.isStatic
        } {
            instructionsLocations += MethodLocation(classFileLocation, method)
        }

        IndexedSeq(instructionsLocations);
    }
}
