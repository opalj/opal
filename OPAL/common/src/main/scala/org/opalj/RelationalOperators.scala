/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

/**
 * The standard relational operators defined in the Java Virtual Machine Specification/
 * Java Language Specification.
 *
 * @author Michael Eichberg
 */
object RelationalOperators extends Enumeration {

    //
    // Operators to compare int values.
    //
    final val LT = Value("<")
    final val < = LT
    final val GT = Value(">")
    final val > = GT
    final val LE = Value("<=")
    final val <= = LE
    final val GE = Value(">=")
    final val >= = GE

    //
    // Operators to compare int and reference values.
    //
    final val EQ = Value("==")
    final val == = EQ
    final val NE = Value("!=")
    final val != = NE

    //
    // Operators to compare floating point numbers.
    //
    final val CMPG = Value("cmpg")
    final val CMPL = Value("cmpl")

    //
    // Operators to compare long values.
    //
    final val CMP = Value("cmp")

}
