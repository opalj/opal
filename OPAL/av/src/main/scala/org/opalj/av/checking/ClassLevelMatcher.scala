/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

import org.opalj.br.ClassFile
import org.opalj.br.analyses.SomeProject

/**
 * A class level matcher matches classes and all methods and fields defined by the
 * respective classes.
 *
 * @author Michael Eichberg
 * @author Marco Torsello
 */
trait ClassLevelMatcher extends SourceElementsMatcher {

    def doesMatch(classFile: ClassFile)(implicit project: SomeProject): Boolean

}
