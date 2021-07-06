/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll

import org.bytedeco.llvm.LLVM.{LLVMBasicBlockRef}

case class BasicBlock(ref: LLVMBasicBlockRef)
