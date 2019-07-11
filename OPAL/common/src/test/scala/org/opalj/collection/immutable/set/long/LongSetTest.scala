/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable
package set 
package long

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
//import org.scalacheck.Prop.classify
import org.scalacheck.Prop.BooleanOperators
//import org.scalacheck.Gen
//import org.scalacheck.Arbitrary
import org.scalatest.Matchers
import org.scalatest.FunSpec

import org.opalj.util.PerformanceEvaluation


@RunWith(classOf[JUnitRunner])
abstract class LongSetTest extends FunSpec with Matchers {

    def empty : LongSet

    // The following methods should be implemented using the most fitting
    // factory methods.
    def create(v1 : Long) : LongSet 
    def create(v1 : Long,v2 : Long) : LongSet 
    def create(v1 : Long,v2 : Long, v3 : Long) : LongSet 
    def create(v1 : Long,v2 : Long, v3 : Long, v4: Long) : LongSet 

    describe("the sets created by the factory methods which take four values") {

        it("should contain all values if the values are distinct") {
            assert(create(1, 2, 3, 4).size == 4)
            assert(create(256, 512, 1024, 2048).size == 4)
            assert(create(0, 1, 10, 1000000).size == 4)
            assert(create(1110, 11, 10, 1).size == 4)
        }

        it("should contain only three values if two values are equal") {
            assert(create(1, 2, 3, 2).size == 3)
            assert(create(1, 1, 3, 2).size == 3)
            assert(create(1, 2, 3, 3).size == 3)
            assert(create(1, 2, 3, 1).size == 3)
        }

        it("should contain only two values if three values are equal") {
            assert(create(1, 2, 2, 2).size == 2)
            assert(create(1, 1, 2, 1).size == 2)
            assert(create(1, 2, 2, 2).size == 2)
            assert(create(2, 2, 2, 1).size == 2)
            assert(create(2, 2, 1, 2).size == 2)
        }

        it("should contain only one value if all values are equal") {
            assert(create(2, 2, 2, 2).size == 1)
        }
    }

    describe("the sets created by the factory methods which take three values") {

        it("should contain all values if the values are distinct") {
            assert(create(1, 2, 4).size == 3)
            assert(create(256, 1024, 2048).size == 3)
            assert(create(0, 1, 1000000).size == 3)
            assert(create(1110, 11, 1).size == 3)
        }

        it("should contain only two values if two values are equal") {
            assert(create(1110, 11, 1).size == 3)
            assert(create(1, 2, 2).size == 2)
            assert(create(1, 1, 2).size == 2)
            assert(create(1, 2, 2).size == 2)
            assert(create(2, 1, 2).size == 2)
            assert(create(2, 2, 1).size == 2)
        }

        it("should contain only one value if all values are equal") {
            assert(create(2, 2, 2).size == 1)
        }
    }

    describe("the sets created by the factory methods which take two values") {

        it("should contain all values if the values are distinct") {
            assert(create(1, 2).size == 2)
            assert(create(256, 2048).size == 2)
            assert(create(0, 1000000).size == 2)
            assert(create(1110, 11).size == 2)
        }

        it("should contain only one value if all values are equal") {
            assert(create(2, 2).size == 1)
        }
    }

    describe("regression tests") {

        val fixtures = List[List[Long]](
            List[Long](4414074060632414370L, 1896250972871104879L, -4468262829510781048L, 3369759390166412338L, 3433954040001057900L, -5360189778998759153L, -4455613594770698331L, 7795367189183618087L, 7342745861545843810L, -938149705997478263L, -7298104853677454976L, 4601242874523109082L, 4545666121642261549L, 2117478629717484238L),
            List[Long](-92276, -76687, -1003, 39908),
            List[Long](-149916L, -102540L, -118018L, -91539L, 0L),
            List[Long](8192 , 16384 , 32768 , 65536 , 131072),
            List[Long](-149831, -143246, -110997, -103241, -100192, -91362, -14553, -10397, -2126, -628, 8184, 13255, 39973),
            List[Long](-103806, -99428, -15784, -6124, 48020),
            List[Long](-134206, -128016, -124763, -106014, -99624, -97374, -90508, -79349, -77213, -20404, 4063, 6348, 14217, 21395, 23943, 25328, 30684, 33875)
        )

        for { fixture ← fixtures } {
            it("should return true for all values of the test fixture: "+fixture) {
                val oLongSet = fixture.foldLeft(empty)((c, n) ⇒ c + n)
                var notFound = List.empty[Long]
                fixture.foreach(v ⇒ if (!oLongSet.contains(v)) notFound ::= v)
                if (notFound.nonEmpty) {
                    val allMissed = notFound.map(v => s"$v(${v.toBinaryString})" )
                    fail(allMissed.mkString(s"the set $oLongSet didn't contain: ", ", ",""))
                }
            }
        }
    }
}