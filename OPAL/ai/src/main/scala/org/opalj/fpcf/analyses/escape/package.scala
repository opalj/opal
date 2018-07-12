/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.tac.DUVar

package object escape {

    type V = DUVar[(Domain with RecordDefUse)#DomainValue]

}
