/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ide
package ifds
package integration

import org.opalj.fpcf.Entity
import org.opalj.ide.ifds.problem.IFDSValue
import org.opalj.ide.integration.IDEPropertyMetaInformation
import org.opalj.ide.problem.IDEFact

/**
 * Interface for property meta information for IFDS problems based on an IDE problem
 */
trait IFDSPropertyMetaInformation[Fact <: IDEFact, Statement, Callable <: Entity]
    extends IDEPropertyMetaInformation[Fact, IFDSValue, Statement, Callable]
