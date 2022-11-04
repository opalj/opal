/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

import org.opalj.br._
import org.opalj.br.analyses.SomeProject

import scala.collection.immutable

/**
 * A source element matcher determines a set of source elements that matches a given query.
 *
 * @author Michael Eichberg
 * @author Marco Torsello
 */
trait SourceElementsMatcher extends (SomeProject => immutable.Set[VirtualSourceElement]) { left =>

    final def apply(project: SomeProject): immutable.Set[VirtualSourceElement] = extension(project)

    def extension(implicit project: SomeProject): immutable.Set[VirtualSourceElement]

    def and(right: SourceElementsMatcher): SourceElementsMatcher = {
        new SourceElementsMatcher {
            def extension(implicit project: SomeProject) = {
                left.extension ++ right.extension
            }

            override def toString() = s"($left and $right)"
        }
    }

    def except(right: SourceElementsMatcher): SourceElementsMatcher = {
        new SourceElementsMatcher {
            def extension(implicit project: SomeProject) = {
                left.extension -- right.extension
            }

            override def toString() = s"($left except $right)"
        }
    }
}
