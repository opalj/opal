/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_PRIVATE
import org.opalj.bi.ACC_PROTECTED
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.FieldAccessInformationKey

import scala.collection.immutable.ArraySeq

/**
 * Counts how often fields are accessed.
 *
 * @author Michael Eichberg
 */
class FieldAccessStatistics(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    override def featureIDs: List[String] = {
        List(
            /*0*/ "unused private fields",
            /*1*/ "unused package visible fields",
            /*2*/ "unused protected fields",
            /*3*/ "unused public fields",
            /*4*/ "package visible fields\nonly used by defining type",
            /*5*/ "protected fields\nonly used by defining type",
            /*6*/ "public fields\nonly used by defininig type "
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(da.ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {
        val locations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        val fieldAccessInformation = project.get(FieldAccessInformationKey)
        import fieldAccessInformation.isAccessed
        import fieldAccessInformation.allAccesses

        for {
            cf <- project.allProjectClassFiles
            classFileLocation = ClassFileLocation(project, cf)
            field <- cf.fields
            fieldType = field.fieldType
            if !fieldType.isBaseType ||
                (field.fieldType ne ObjectType.String) ||
                !(field.isStatic && field.isFinal)
        } {
            val category =
                if (!isAccessed(field)) {
                    field.visibilityModifier match {
                        case Some(ACC_PRIVATE)   => 0
                        case None                => 1
                        case Some(ACC_PROTECTED) => 2
                        case Some(ACC_PUBLIC)    => 3
                    }
                } else if (!field.isPrivate && allAccesses(field).forall(mi => mi._1.classFile eq cf)) {
                    field.visibilityModifier match {
                        case None                => 4
                        case Some(ACC_PROTECTED) => 5
                        case Some(ACC_PUBLIC)    => 6

                        case Some(ACC_PRIVATE) =>
                            throw new UnknownError(s"non private-field $field has private modifier")
                    }
                } else {
                    -1
                }
            if (category != -1) locations(category) += FieldLocation(classFileLocation, field)
        }
        ArraySeq.unsafeWrapArray(locations)
    }
}
