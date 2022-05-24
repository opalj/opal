/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes

import java.net.URL

import org.opalj.da
import org.opalj.br.analyses.Project

/**
 * The resources of the instantiated project.
 *
 * @author Michael Eichberg
 */
case class ProjectInstantiation(
        project:       Project[URL],
        rawClassFiles: Iterable[(da.ClassFile, URL)]
)
