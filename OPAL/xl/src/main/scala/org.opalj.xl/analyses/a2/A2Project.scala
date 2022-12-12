/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package a2

import org.opalj.xl.languages.L2

class A2Project(val entities: Iterable[org.opalj.xl.languages.L2.Function]) {
    def functions: Iterable[L2.Function] =
        List(L2.Function("g", List(L2.Variable("a")),
          List(
            //L2.Assignment(L2.Variable("o"), L2.ForeignFunctionCall(L1, "f", List(L2.Variable("a")))),
            L2.Assignment(L2.Variable("i"), L2.Variable("a")),
            L2.Assignment(L2.Variable("return"), L2.Variable("i"))
          )))

  //  def assignment(name: String): Option[org.opalj.xl.languages.L1.Assignment] =
  //      functions.find(_.name == name)
}

object A2Project {
    def apply(modules_paths: Iterable[String]): A2Project = {
        //val modules = code.modules_paths.map(path => Reader.readIR(path).get)
        val project = new A2Project(Iterable.empty)
        project
    }
}
