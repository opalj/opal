/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funspec.AnyFunSpec

@RunWith(classOf[JUnitRunner])
abstract class LongSetTest extends AnyFunSpec {

    def empty(): LongSet

    // The following methods should be implemented using the most fitting
    // factory methods.
    def create(v1: Long): LongSet
    def create(v1: Long, v2: Long): LongSet
    def create(v1: Long, v2: Long, v3: Long): LongSet
    def create(v1: Long, v2: Long, v3: Long, v4: Long): LongSet

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
            List[Long](-6993563706325782081L, -25, 63, -1),
            List[Long](-148198, -146377, -144713, -136387, -128093, -125412, -114745, -114416, -114042, -111787, -107616, -106292, -104265, -100180, -96310, -85335, -78774, -19351, -9013, -8061, -6281, -6119, -96, 1969, 3393, 8171, 10401, 10672, 24158, 27468, 27866, 29988, 30001, 37426, 46739),
            List[Long](-149888, -148631, -145484, -141562, -140079, -138107, -136334, -134497, -134411, -133903, -130864, -127559, -125917, -125874, -122818, -122662, -121942, -117767, -109611, -109221, -106817, -105853, -98499, -97621, -97268, -95127, -87266, -87167, -84007, -81481, -79385, -78975, -77313, -24933, -24046, -21559, -14486, -13697, -10057, -9088, -8839, -8094, -2799, 4427, 4715, 6171, 9785, 12698, 13743, 14451, 17035, 22533, 23552, 25426, 28095, 28971, 30684, 38784, 39380, 40539, 41679, 43454, 44458, 46205, 47203, 47959, 48225),
            List[Long](-133101, -131537, -110967, -102501, -94607, -89623, -82931, -20999, 1601, 1914, 16349, 16597, 31899, 49328),
            List[Long](-148452, -133542, -119766, -18749, -8976, -8071),
            List[Long](4414074060632414370L, 1896250972871104879L, -4468262829510781048L, 3369759390166412338L, 3433954040001057900L, -5360189778998759153L, -4455613594770698331L, 7795367189183618087L, 7342745861545843810L, -938149705997478263L, -7298104853677454976L, 4601242874523109082L, 4545666121642261549L, 2117478629717484238L),
            List[Long](-143785, -114103, -80816, -4668, 5229, 26264),
            List[Long](-139445, -133367, -106981, -81548, -77199, -75525, -8910, -4517, -2458, 174, 13649, 25930, 33737),
            List[Long](-146501L << 24, -137809L << 24, -92565L << 24, -2585L << 24, 42822L << 24, 43337L << 24),
            List[Long](-146600, -140735, -139854, -129840, -120475, -104855, -103277, -102090, -100994, -100568, -86461, -78635, -19372, -14745, -2214, -1718, 10236, 24057, 25739, 26007, 27050, 34031, 34347, 34872),
            List[Long](-147366, -139048, -116344, -115683, -96550, -94893, -93671, -85883, -81353, -79557, -77003, -76450, -11499, 6020, 9867, 10204, 11359, 30183, 37307, 41127, 42384, 45544, 46243, 49298),
            List[Long](-92276, -76687, -1003, 39908),
            List[Long](-149831, -143246, -110997, -103241, -100192, -91362, -14553, -10397, -2126, -628, 8184, 13255, 39973),
            List[Long](-103806, -99428, -15784, -6124, 48020),
            List[Long](-134206, -128016, -124763, -106014, -99624, -97374, -90508, -79349, -77213, -20404, 4063, 6348, 14217, 21395, 23943, 25328, 30684, 33875),
            List[Long](-149916L, -102540L, -118018L, -91539L, 0L),
            List[Long](8192, 16384, 32768, 65536, 131072)
        )

        for { fixture <- fixtures } {
            val oLongSet = fixture.foldLeft(empty())((c, n) => c + n)
            val otherValues = fixture.map(~_).filter(v => !fixture.contains(v))

            it("should return true for all added values: "+fixture) {
                var notFound = List.empty[Long]
                fixture.foreach(v => if (!oLongSet.contains(v)) notFound ::= v)
                if (notFound.nonEmpty) {
                    val unexpected = notFound.map(v => s"$v(${v.toBinaryString})")
                    fail(unexpected.mkString(s"the set $oLongSet didn't contain: ", ", ", ""))
                }
            }

            it("should return false for values which are not added: "+otherValues) {
                var found = List.empty[Long]
                otherValues.foreach(v => if (oLongSet.contains(v)) found ::= v)
                if (found.nonEmpty) {
                    val unexpected = found.map(v => s"$v(${v.toBinaryString})")
                    fail(unexpected.mkString(s"the set $oLongSet contains: ", ", ", ""))
                }
            }
        }
    }
}