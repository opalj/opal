/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import scala.collection.mutable
import org.opalj.br.analyses.Project
import org.opalj.br.cfg.CFGFactory

/**
 * Extracts basic metric information (Fields/Methods per Class; Classes per Package; etc.).
 *
 * @author Michael Reif
 */
class Metrics(implicit hermes: HermesConfig) extends FeatureQuery {

    /**
     * The unique ids of the extracted features.
     */
    override val featureIDs: Seq[String] = {
        Seq(
            "0 FPC", "1-3 FPC", "4-10 FPC", ">10 FPC", // 0, 1, 2, 3
            "0 MPC", "1-3 MPC", "4-10 MPC", ">10 MPC", // 4, 5, 6, 7
            "1-3 CPP", "4-10 CPP", ">10 CPP", // 8, 9, 10
            "0 NOC", "1-3 NOC", "4-10 NOC", ">10 NOC", //  11, 12, 13, 14
            "linear methods (McCabe)", "2-3 McCabe", "4-10 McCabe", ">10 McCabe" // 15, 16, 17 ,18
        )
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(da.ClassFile, S)]
    ): IterableOnce[Feature[S]] = {

        val classLocations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        class PackageInfo(var classesCount: Int = 0, val location: PackageLocation[S])
        val packagesInfo = mutable.Map.empty[String, PackageInfo]

        val classHierarchy = project.classHierarchy

        for {
            (classFile, source) <- project.projectClassFilesWithSources
            classLocation = ClassFileLocation(source, classFile)
        } {
            // fpc

            classFile.fields.size match {
                case 0            => classLocations(0) += classLocation
                case x if x <= 3  => classLocations(1) += classLocation
                case x if x <= 10 => classLocations(2) += classLocation
                case x            => classLocations(3) += classLocation
            }

            // mpc

            classFile.methods.size match {
                case 0            => classLocations(4) += classLocation
                case x if x <= 3  => classLocations(5) += classLocation
                case x if x <= 10 => classLocations(6) += classLocation
                case x            => classLocations(7) += classLocation
            }

            // noc

            classHierarchy.directSubtypesOf(classFile.thisType).size match {
                case 0            => classLocations(11) += classLocation
                case x if x <= 3  => classLocations(12) += classLocation
                case x if x <= 10 => classLocations(13) += classLocation
                case x            => classLocations(14) += classLocation
            }

            // count the classes per package
            val packageName = classFile.thisType.packageName
            val packageInfo = packagesInfo.getOrElseUpdate(
                packageName,
                new PackageInfo(location = PackageLocation(packageName))
            )
            packageInfo.classesCount += 1

            // McCabe
            classFile.methods foreach { method =>
                CFGFactory(method, project.classHierarchy) foreach { cfg =>
                    val methodLocation = MethodLocation(classLocation, method)
                    val bbs = cfg.reachableBBs
                    val edges = bbs.foldLeft(0) { (res, node) =>
                        res + node.successors.size
                    }
                    val mcCabe = edges - bbs.size + 2
                    mcCabe match {
                        case 1            => classLocations(15) += methodLocation
                        case x if x <= 3  => classLocations(16) += methodLocation
                        case x if x <= 10 => classLocations(17) += methodLocation
                        case x            => classLocations(18) += methodLocation
                    }
                }
            }
        }

        packagesInfo.values foreach { pi =>
            pi.classesCount match {
                case x if x <= 3  => classLocations(8) += pi.location
                case x if x <= 10 => classLocations(9) += pi.location
                case x            => classLocations(10) += pi.location
            }
        }

        for { (featureID, featureIDIndex) <- featureIDs.iterator.zipWithIndex } yield {
            Feature[S](featureID, classLocations(featureIDIndex))
        }
    }
}
