/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll

import org.bytedeco.llvm.global.LLVM.LLVMDumpModule


object LLPlayground {

    def main(args: Array[String]): Unit = {
        val module = Reader.readIR("./OPAL/ll/src/test/resources/org/opalj/ll/test.ll").get
        LLVMDumpModule(module)
    }
}
