/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package cli

import org.rogach.scallop.flagConverter

object MultiProjectsCommand extends PlainCommand[Boolean] {
    override val name: String = "multiProjects"
    override val description: String = "Analyzes multiple projects in the subdirectories of --cp"
}
