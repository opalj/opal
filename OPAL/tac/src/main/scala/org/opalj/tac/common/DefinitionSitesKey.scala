/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package common

import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject

/**
 * The [[org.opalj.br.analyses.ProjectInformationKey]] to retrieve the
 * [[DefinitionSites]] object for a project.
 *
 * @author Dominik Helm
 * @author Florian Kübler
 */
object DefinitionSitesKey extends ProjectInformationKey[DefinitionSites, Nothing] {

    override def requirements(project: SomeProject): ProjectInformationKeys = Seq.empty

    override def compute(project: SomeProject): DefinitionSites = new DefinitionSites(project)
}
