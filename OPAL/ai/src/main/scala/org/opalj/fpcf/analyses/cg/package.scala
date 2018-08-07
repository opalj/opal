/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.tac.DUVar
import org.opalj.value.KnownTypedValue

package object cg {
    type V = DUVar[KnownTypedValue]
}
