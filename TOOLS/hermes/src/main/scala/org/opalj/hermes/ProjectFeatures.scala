/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes

import javafx.beans.property.StringProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.ObjectProperty

/**
 * The feature objects associated with every project.
 *
 * @author Michael Eichberg
 */
case class ProjectFeatures[S](
        projectConfiguration: ProjectConfiguration,
        featureGroups:        Seq[(FeatureQuery, Seq[ObjectProperty[Feature[S]]])]
) {

    /** The project's unique id. */
    final val id: StringProperty = {
        new SimpleStringProperty(projectConfiguration, "project", projectConfiguration.id)
    }

    final val features: Seq[ObjectProperty[Feature[S]]] = featureGroups.flatMap(_._2)

}
