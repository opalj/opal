/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

import scala.util.matching.Regex

/**
 * Matches a (binary) name of a file, method or class.
 *
 * @author Michael Eichberg
 */
trait NamePredicate extends (String => Boolean)

/**
 * @author Marco Torsello
 * @author Michael Eichberg
 */
case class Equals(name: BinaryString) extends NamePredicate {

    def apply(that: String): Boolean = {
        this.name.asString == that
    }
}

/**
 * @author Michael Eichberg
 */
case class StartsWith(name: BinaryString) extends NamePredicate {

    def apply(that: String): Boolean = {
        that.startsWith(this.name.asString)
    }
}

/**
 * Matches name of class, fields and methods based on their name.
 *
 * '''The name is matched against the binary notation.'''
 *
 * @author Michael Eichberg
 */
case class RegexNamePredicate(matcher: Regex) extends NamePredicate {

    def apply(otherName: String): Boolean = {
        matcher.findFirstIn(otherName).isDefined
    }
}
