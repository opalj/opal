/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
        rawClassFiles:        Iterable[(da.ClassFile, S)]
    ): IterableOnce[Feature[S]] = {

        val methodLocations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        for {
            (classFile, source) <- project.projectClassFilesWithSources
            if !isInterrupted()
            classLocation = ClassFileLocation(source, classFile)
            m <- classFile.methods
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

        for { (featureID, featureIDIndex) <- featureIDs.iterator.zipWithIndex } yield {
            Feature[S](featureID, methodLocations(featureIDIndex))
        }
    }
}
