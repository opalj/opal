/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll

import org.opalj.ll.llvm.Reader

object LLPlayground {

    def main(args: Array[String]): Unit = {
        val module = Reader.readIR("./OPAL/ll/src/test/resources/org/opalj/ll/test_jsmn.ll").get
        val function = module.get_function("jsmn_parse_string")
        println(function.name)
        function.viewLLVMCFG(false)
        function.viewCFG()
    }
}
