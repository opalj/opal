/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ide.ifds.integration

import org.opalj.ide.ifds.problem.IFDSValue
import org.opalj.ide.integration.IDEPropertyMetaInformation
import org.opalj.ide.problem.IDEFact

/**
 * Interface for property meta information for IFDS problems based on an IDE problem
 */
trait IFDSPropertyMetaInformation[Statement, Fact <: IDEFact]
    extends IDEPropertyMetaInformation[Statement, Fact, IFDSValue]
