/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import org.opalj.br.analyses.Project
import org.opalj.br.MethodWithBody

/**
 * Classifies class file elements which contain debug information.
 *
 * @note  The "SourceDebugExtension" attribute is ignored as it is generally not used.
 *
 * @author Michael Eichberg
 */
class DebugInformation(implicit hermes: HermesConfig) extends FeatureQuery {

    override def featureIDs: IndexedSeq[String] = {
        IndexedSeq(
            /*0*/ "Class File With\nSource Attribute",
            /*1*/ "Method With\nLine Number Table",
            /*2*/ "Method With\nLocal Variable Table",
            /*3*/ "Method With\nLocal Variable Type Table"
        )
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(da.ClassFile, S)]
    ): IterableOnce[Feature[S]] = {
        val locations = Array.fill(4)(new LocationsContainer[S])

        for {
            (classFile, source) <- project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
        } {
            if (classFile.sourceFile.isDefined) locations(0) += classFileLocation

            for {
                method @ MethodWithBody(body) <- classFile.methods
                methodLocation = MethodLocation(classFileLocation, method)
            } {
                if (body.localVariableTable.isDefined) locations(1) += methodLocation
                if (body.localVariableTypeTable.nonEmpty) locations(2) += methodLocation
                if (body.lineNumberTable.isDefined) locations(3) += methodLocation
            }
        }

        for { (locations, index) <- locations.iterator.zipWithIndex } yield {
            Feature[S](featureIDs(index), locations)
        }
    }
}
