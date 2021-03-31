/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.issues

import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

/**
 * Tests the `toIDL` method of [[Relevance]].
 *
 * @author Lukas Berg
 */
@RunWith(classOf[JUnitRunner])
class RelevanceIDLTest extends AnyFlatSpec with Matchers {

    import IDLTestsFixtures._

    behavior of "the toIDL method"

    it should "return a valid IDLJson object for Relevance.OfNoRelevance" in {
        val relevance = Relevance.OfNoRelevance
        relevance.toIDL should be(toIDL(Relevance.OfNoRelevance))
    }

    it should "return a valid IDLJson object for Relevance.High" in {
        val relevance = Relevance.High
        relevance.toIDL should be(toIDL(Relevance.High))
    }
}
