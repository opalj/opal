/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.br.PC
import org.opalj.br.fpcf.properties.Context
import org.opalj.tac.DUVar
import org.opalj.value.ValueInformation

package object alias {

    type V = DUVar[ValueInformation]

    type AllocationSite = (Context, PC)
}
