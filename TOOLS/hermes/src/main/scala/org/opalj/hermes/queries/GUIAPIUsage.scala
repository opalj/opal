/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import org.opalj.br.analyses.Project
import org.opalj.da.CONSTANT_Utf8_info
import org.opalj.da.ClassFile

/**
 * Scans a class file's constant pool to check whether it refers to packages that belong to an API
 * for graphical user interfaces. The current analysis supports:
 *  - JavaFX (javafx.)
 *  - SWT (org.eclipse.swt)
 *  - Swing (javax.swing)
 *  - AWT (java.awt)
 *
 * @author Michael Reif
 */
class GUIAPIUsage(implicit hermes: HermesConfig) extends FeatureQuery {

    override val featureIDs: List[String] = {
        List(
            /* 0 */ "JavaFX",
            /* 1 */ "SWT",
            /* 2 */ "Swing",
            /* 3 */ "AWT"
        )
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(ClassFile, S)]
    ): IterableOnce[Feature[S]] = {

        val locations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        for {
            (classFile, source) <- rawClassFiles
            location = ClassFileLocation(Some(source), classFile.thisType.asJava)
            CONSTANT_Utf8_info(_, entry) <- classFile.constant_pool
        } {
            if (entry.startsWith("javafx/")) {
                // note: package "javafx" is empty,
                locations(0) += location
            } else if (entry.startsWith("org/eclipse/swt")) {
                locations(1) += location
            } else if (entry.startsWith("javax/swing")) {
                locations(2) += location
            } else if (entry.startsWith("java/awt")) {
                locations(3) += location
            }
        }

        for { (featureID, featureIDIndex) <- featureIDs.iterator.zipWithIndex } yield {
            Feature[S](featureID, locations(featureIDIndex))
        }
    }
}
