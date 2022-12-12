/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package a0

import org.opalj.xl.languages.GlobalVariable
import org.opalj.xl.languages.L0
import org.opalj.xl.languages.L1

class A0Project(val entities: Iterable[org.opalj.xl.languages.L0.Assignment]) {
    def functions: Iterable[org.opalj.xl.languages.L0.Function] =
        List(L0.Function("main", List.empty, List(
          L0.Assignment(L0.Variable("source"), L0.Num(5)),
          L0.Assignment(L0.Variable("b"), L0.Num(8)),
          L0.Assignment(L0.Variable("c"), L0.Variable("source")),
          L0.Assignment(GlobalVariable("d"), L0.Variable("c")),
          L0.Assignment(
            L0.Variable("e"),
            L0.ForeignFunctionCall(
              L1,
              "f",
              List(L0.Variable("c"))
            )
          ),
          L0.Assignment(L0.Variable("return"), L0.Variable("c"))
        )
        ))
}

object A0Project {
    def apply(modules_paths: Iterable[String]): A0Project = {
        val project = new A0Project(Iterable.empty)
        project
    }
}
