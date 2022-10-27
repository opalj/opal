/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package common

import org.opalj.br.analyses.JavaProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.JavaProjectInformationKey

/**
 * The [[JavaProjectInformationKey]] to retrieve the
 * [[DefinitionSites]] object for a project.
 *
 * @author Dominik Helm
 * @author Florian Kübler
 */
object DefinitionSitesKey extends JavaProjectInformationKey[DefinitionSites, Nothing] {

    override def requirements(project: SomeProject): JavaProjectInformationKeys = Seq.empty

    override def compute(project: SomeProject): DefinitionSites = new DefinitionSites(project)
}
