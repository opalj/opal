/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import org.opalj.hermes.Feature
import org.opalj.hermes.FeatureQuery
import org.opalj.hermes.ProjectConfiguration

import org.opalj.da.ClassFile
import org.opalj.br.analyses.Project

/**
 * Computes the size of the inheritance tree for each class of a project and then assigns the
 * class to its respective category.
 *
 * Here, the size is measured by counting all super types (interfaces and classes!)
 *
 * @author Ben Hermann
 * @author Michael Eichberg
 */
class SizeOfInheritanceTree(implicit hermes: HermesConfig) extends FeatureQuery {

    override val featureIDs: List[String] = {
        List(
            /*0*/ "Very Small Inheritance Tree", // [0 ... 1 x CategorySize)
            /*1*/ "Small Inheritance Tree", // [1 x CategorySize ... 2 x CategorySize)
            /*2*/ "Medium Inheritance Tree", // [2 x CategorySize ... 3 x CategorySize)
            /*3*/ "Large Inheritance Tree", // [3 x CategorySize ... 4 x CategorySize)
            /*4*/ "Very Large Inheritance Tree", // [4 x CategorySize ... 5 x CategorySize)
            /*5*/ "Huge Inheritance Tree", // [5 x CategorySize ... 6 x Int.MaxValue)

            /*6*/ "Size of the Inheritance Tree Unknown" // The project is not complete
        )
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(ClassFile, S)]
    ): IterableOnce[Feature[S]] = {
        val classHierarchy = project.classHierarchy
        import classHierarchy.isSupertypeInformationComplete

        /* Determines the size for each category. */
        val CategorySize: Int = 3 // TODO read from project config file

        val features = Array.fill[LocationsContainer[S]](featureIDs.size)(new LocationsContainer[S])
        var classCount = 0
        var sumOfSizeOfInheritanceTrees = 0
        classHierarchy.foreachKnownType { t =>
            if (project.isProjectType(t)) {
                val l = ClassFileLocation(project, t)
                classHierarchy.supertypeInformation(t) match {
                    case Some(supertypeInformation) if isSupertypeInformationComplete(t) =>
                        val sizeOfInheritanceTree = supertypeInformation.size
                        features(Math.min(sizeOfInheritanceTree / CategorySize, 5)) += l
                        classCount += 1
                        sumOfSizeOfInheritanceTrees += sizeOfInheritanceTree
                    case _ /* None or <incomplete> */ =>
                        features(6) += l
                }
            }
        }

        projectConfiguration.addStatistic(
            "⟨SizeOfInheritanceTree⟩",
            if (classCount != 0) {
                sumOfSizeOfInheritanceTrees.toDouble / classCount.toDouble
            } else {
                0D
            }
        )

        for { (featureID, featureIDIndex) <- featureIDs.iterator.zipWithIndex } yield {
            Feature[S](featureID, features(featureIDIndex))
        }

    }

}
