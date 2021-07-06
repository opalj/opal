/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll

object LLPlayground {

    def main(args: Array[String]): Unit = {
        val module = Reader.readIR("./OPAL/ll/src/test/resources/org/opalj/ll/test.ll").get
        println(module.repr)
        println(module.functions.map(f => f.name).toList)
        println(module.functions.map(f => f.repr).toList)
    }
}
