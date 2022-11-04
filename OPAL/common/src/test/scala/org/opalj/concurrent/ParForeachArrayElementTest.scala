/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package concurrent

import java.util.concurrent.atomic.AtomicInteger

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.util.control.ControlThrowable

/**
 * Tests `parForeachArrayElement`.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ParForeachArrayElementTest extends AnyFunSpec with Matchers {

    describe("parForeachArrayElement") {

        it("it should collect all exceptions that are thrown if we just use one thread") {
            val data = Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
            try {
                parForeachArrayElement(data, 1) { e => throw new RuntimeException(e.toString) }
            } catch {
                case ce: ConcurrentExceptions =>
                    assert(ce.getSuppressed.length == 16)
                    assert(ce.getSuppressed.forall(_.isInstanceOf[RuntimeException]))
            }
        }

        it("it should collect all exceptions that are thrown if we use multiple threads") {
            val data = Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
            try {
                parForeachArrayElement(data, 8) { e => throw new RuntimeException(e.toString) }
            } catch {
                case ce: ConcurrentExceptions =>
                    assert(ce.getSuppressed.length == 16)
                    assert(ce.getSuppressed.forall(_.isInstanceOf[RuntimeException]))
            }
        }

        it("it should catch a non-local return and report it") {
            val processed = new AtomicInteger(0)
            def test(): Unit = {
                val data = Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
                try {
                    parForeachArrayElement(data, 8) { e =>
                        if (e == 7) return ; else processed.incrementAndGet()
                    }
                } catch {
                    case ce: ConcurrentExceptions =>
                        assert(ce.getSuppressed().length == 1)
                        assert(ce.getSuppressed()(0).getCause.isInstanceOf[ControlThrowable])
                }
            }
            test()
            assert(processed.get() == 15)
        }

    }
}
