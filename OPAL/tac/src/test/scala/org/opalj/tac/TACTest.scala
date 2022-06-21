/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import java.util.Arrays
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

/**
 * Common superclass of all TAC unit tests.
 *
 * @author Michael Eichberg
 */
private[tac] class TACTest extends AnyFunSpec with Matchers {

    def compareStatements[V <: Var[V]](
        expectedStmts: IndexedSeq[Stmt[V]],
        actualStmts:   IndexedSeq[Stmt[V]]
    ): Unit = {
        compareStatements(expectedStmts.toArray, actualStmts.toArray)
    }

    def compareStatements[V <: Var[V]](
        expectedStmts: Array[Stmt[V]],
        actualStmts:   Array[Stmt[V]]
    ): Unit = {
        val expected = expectedStmts.asInstanceOf[Array[Object]]
        val actual = actualStmts.asInstanceOf[Array[Object]]
        if (!Arrays.equals(expected: Array[Object], actual: Array[Object])) {
            val message =
                actualStmts.zip(expectedStmts).
                    filter(p => p._1 != p._2).
                    map(p => "\t"+p._1+"\n\t<=>[Expected:]\n\t"+p._2+"\n").
                    mkString("Differences:\n", "\n", "\n")
            fail(message)
        }
    }
}
