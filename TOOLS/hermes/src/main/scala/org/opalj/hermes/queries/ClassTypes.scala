/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import org.opalj.br.analyses.Project

/**
 * Counts which kinds of class types are actually defined.
 *
 * @author Michael Eichberg
 */
class ClassTypes(implicit hermes: HermesConfig) extends FeatureQuery {

    override val featureIDs: List[String] = {
        List(
            /*0*/ "(concrete) classes",
            /*1*/ "abstract classes",
            /*2*/ "annotations",
            /*3*/ "enumerations",
            /*4*/ "marker interfaces",
            /*5*/ "simple functional interfaces\n(single abstract method (SAM) interface)",
            /*6*/ "non-functional interface\nwith default methods (Java >8)",
            /*7*/ "non-functional interface\nwith static methods (Java >8)",
            /*8*/ "(standard) interface",
            /*9*/ "module (Java >9)"
        )
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(da.ClassFile, S)]
    ): IterableOnce[Feature[S]] = {

        val classTypesLocations = Array.fill(10)(new LocationsContainer[S])

        val functionalInterfaces = project.functionalInterfaces

        for {
            (classFile, source) <- project.projectClassFilesWithSources
            if !isInterrupted()
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

                // In the following we try to distinguish the different types of
                // interfaces that are usually distinguished from a developer point-of-view.
                val explicitlyDefinedMethods = classFile.methods.filter(_.name != "<clinit>")

                if (explicitlyDefinedMethods.isEmpty && classFile.interfaceTypes.isEmpty) {
                    // => MarkerInterface (even if it defines complex constants)
                    classTypesLocations(4) += location

                } else if (functionalInterfaces.contains(classFile.thisType)) {
                    // we have found a "true" functional interface
                    classTypesLocations(5) += location
                } else {
                    var isJava8Interface = false
                    if (explicitlyDefinedMethods.exists(m => !m.isAbstract && !m.isStatic)) {
                        classTypesLocations(6) += location
                        isJava8Interface = true
                    }
                    if (explicitlyDefinedMethods.exists(m => m.isStatic)) {
                        classTypesLocations(7) += location
                        isJava8Interface = true
                    }
                    if (!isJava8Interface) {
                        classTypesLocations(8) += location
                    }
                }

            } else if (classFile.isModuleDeclaration) {
                classTypesLocations(9) += location
            }
        }

        for { (featureID, featureIDIndex) <- featureIDs.iterator.zipWithIndex } yield {
            Feature[S](featureID, classTypesLocations(featureIDIndex))
        }
    }
}
