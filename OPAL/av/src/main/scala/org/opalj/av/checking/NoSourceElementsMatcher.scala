/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

import scala.collection.immutable
import org.opalj.br.analyses.SomeProject
import org.opalj.br.VirtualSourceElement

/**
 * A source element matcher that matches no elements.
 *
 * @author Michael Eichberg
 */
case object NoSourceElementsMatcher extends SourceElementsMatcher {

    def extension(implicit project: SomeProject): immutable.Set[VirtualSourceElement] = Set.empty

}

