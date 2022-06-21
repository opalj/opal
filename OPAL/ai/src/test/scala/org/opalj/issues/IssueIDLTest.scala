/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package issues

import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.JsString
import play.api.libs.json.Json

/**
 * Tests the toIDL method of Issue
 *
 * @author Lukas Berg
 */
@RunWith(classOf[JUnitRunner])
class IssueIDLTest extends AnyFlatSpec with Matchers {

    import IDLTestsFixtures._

    behavior of "the toIDL method"

    it should "return a valid issue description for a most basic Issue" in {
        val issue = Issue(
            null,
            Relevance.OfNoRelevance,
            "foo",
            Set.empty,
            Set.empty,
            Seq(simplePackageLocation)
        )

        issue.toIDL should be(Json.obj(
            "analysis" -> JsString(null),
            "relevance" -> toIDL(Relevance.OfNoRelevance),
            "summary" -> "foo",
            "categories" -> Json.arr(),
            "kinds" -> Json.arr(),
            "details" -> Json.arr(),
            "locations" -> Json.arr(simplePackageLocationIDL)
        ))
    }

    it should "return a valid issue description for an Issue which specifies the underlying analysis" in {
        val issue = Issue(
            "bar",
            Relevance.OfNoRelevance,
            "foo",
            Set.empty,
            Set.empty,
            Seq(simplePackageLocation)
        )

        issue.toIDL should be(Json.obj(
            "analysis" -> "bar",
            "relevance" -> toIDL(Relevance.OfNoRelevance),
            "summary" -> "foo",
            "categories" -> Json.arr(),
            "kinds" -> Json.arr(),
            "details" -> Json.arr(),
            "locations" -> Json.arr(simplePackageLocationIDL)
        ))
    }

    it should "return a valid issue description for an Issue which defines some categories" in {
        val issue = Issue(
            null,
            Relevance.OfNoRelevance,
            "bar",
            Set("a", "b"),
            Set.empty,
            Seq(simplePackageLocation)
        )

        issue.toIDL should be(Json.obj(
            "analysis" -> JsString(null),
            "relevance" -> toIDL(Relevance.OfNoRelevance),
            "summary" -> "bar",
            "categories" -> Json.arr("a", "b"),
            "kinds" -> Json.arr(),
            "details" -> Json.arr(),
            "locations" -> Json.arr(simplePackageLocationIDL)
        ))
    }

    it should "return a valid issue description for an Issue which defines the kinds" in {
        val issue = Issue(
            null,
            Relevance.OfNoRelevance,
            "foo",
            Set.empty,
            Set("c", "d"),
            Seq(simplePackageLocation)
        )

        issue.toIDL should be(Json.obj(
            "analysis" -> JsString(null),
            "relevance" -> toIDL(Relevance.OfNoRelevance),
            "summary" -> "foo",
            "categories" -> Json.arr(),
            "kinds" -> Json.arr("c", "d"),
            "details" -> Json.arr(),
            "locations" -> Json.arr(simplePackageLocationIDL)
        ))
    }

    it should "return a valid issue description for an Issue which specifies further details" in {
        val issue = Issue(
            null,
            Relevance.OfNoRelevance,
            "foo",
            Set.empty,
            Set.empty,
            Seq(simplePackageLocation),
            Seq(simpleOperands, simpleLocalVariables)
        )

        issue.toIDL should be(Json.obj(
            "analysis" -> JsString(null),
            "relevance" -> toIDL(Relevance.OfNoRelevance),
            "summary" -> "foo",
            "categories" -> Json.arr(),
            "kinds" -> Json.arr(),
            "details" -> Json.arr(simpleOperandsIDL, simpleLocalVariablesIDL),
            "locations" -> Json.arr(simplePackageLocationIDL)
        ))
    }

    it should "return a valid issue description for an Issue which specifies all standard attributes" in {
        val issue = Issue(
            "foo",
            Relevance.OfUtmostRelevance,
            "bar",
            Set("b", "a"),
            Set("d", "c"),
            Seq(simplePackageLocation),
            Seq(simpleLocalVariables, simpleOperands)
        )

        issue.toIDL should be(Json.obj(
            "analysis" -> "foo",
            "relevance" -> toIDL(Relevance.OfUtmostRelevance),
            "summary" -> "bar",
            "categories" -> Json.arr("b", "a"),
            "kinds" -> Json.arr("d", "c"),
            "details" -> Json.arr(simpleLocalVariablesIDL, simpleOperandsIDL),
            "locations" -> Json.arr(simplePackageLocationIDL)
        ))
    }
}
