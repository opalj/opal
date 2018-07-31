/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package dataflow
package spec

import scala.collection.{Map, Set}
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject

case class MethodsMatcher(
        matcher: PartialFunction[Method, Set[Int]]
) extends AValueLocationMatcher {

    def apply(project: SomeProject): Map[Method, Set[Int]] = {
        val map = scala.collection.mutable.AnyRefMap.empty[Method, Set[Int]]
        for {
            classFile ← project.allProjectClassFiles
            method ← classFile.methods
            if method.body.isDefined
            if matcher.isDefinedAt(method)
        } {
            map.update(method, matcher(method))
        }
        map
    }
}
