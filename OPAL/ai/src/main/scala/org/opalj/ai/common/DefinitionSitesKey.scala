/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package common

import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject

/**
 * The [[org.opalj.br.analyses.ProjectInformationKey]] to retrieve the
 * [[DefinitionSites]] object for a project.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
object DefinitionSitesKey extends ProjectInformationKey[DefinitionSites, Nothing] {

    override protected def requirements: ProjectInformationKeys = Seq.empty

    override protected def compute(project: SomeProject): DefinitionSites = {
        new DefinitionSites(project)
    }
}
