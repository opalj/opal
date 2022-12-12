/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package a1

import org.opalj.xl.languages.L1
import org.opalj.xl.languages.L2

class A1Project(val entities: Iterable[org.opalj.xl.languages.L1.Function]) {
    def functions: Iterable[org.opalj.xl.languages.L1.Function] =
        List(
          L1.Function(
            "f",
            List(L1.Variable("a"), L1.Variable("b")),
            List(
              L1.Assignment(L1.Variable("m"), L1.ForeignFunctionCall(L2, "g", List(L1.Variable("a")))),
          L1.Assignment(L1.Variable("return"), L1.Variable("m")))))

  //  def assignment(name: String): Option[org.opalj.xl.languages.L1.Assignment] =
  //      functions.find(_.name == name)
}

object A1Project {
    def apply(modules_paths: Iterable[String]): A1Project = {
        //val modules = code.modules_paths.map(path => Reader.readIR(path).get)
        val project = new A1Project(Iterable.empty)

        project
    }
}
