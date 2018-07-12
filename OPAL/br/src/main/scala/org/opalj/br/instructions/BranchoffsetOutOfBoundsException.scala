/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

case class BranchoffsetOutOfBoundsException(
        labeledInstruction: LabeledInstruction
) extends RuntimeException(
    s"resolving the branchoffset of $labeledInstruction resulted in an invalid branchoffset"
)
