/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.trash.bridges

import org.opalj.fpcf.Entity
import org.opalj.xl.languages.L

abstract class Bridge {
  def translate(entity:Entity): (L, String, List[Entity], List[Entity])
}
