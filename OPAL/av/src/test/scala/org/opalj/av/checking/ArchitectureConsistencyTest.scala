/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.av
package checking

import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.junit.JUnitRunner

import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.BooleanValue
import org.opalj.br.StringValue
import org.opalj.br.reader.Java8Framework.ClassFiles

/**
 * Tests for architectural Specifications.
 *
 * The architecture is defined w.r.t. the "Mathematics test classes".
 *
 * @author Samuel Beracasa
 * @author Marco Torsello
 */
@RunWith(classOf[JUnitRunner])
class ArchitectureConsistencyTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

    val project = ClassFiles(locateTestResources("classfiles/mathematics.jar", "av"))

    def testEnsemblesAreNonEmpty(specification: Specification): Unit = {
        specification.ensembles.foreach { e =>
            val (ensembleID, (matcher, extent)) = e
            if (ensembleID != Symbol("Empty") && extent.isEmpty)
                fail(s"$ensembleID didn't match any elements ($matcher)")
        }
    }

    behavior of "the Architecture Validation Framework when checking architectural dependencies"

    /*
     * outgoing is_only_allowed_to constraint validations
     */
    it should "correctly validate a valid specification using is_only_allowed_to_use constraints" in {
        val specification = new Specification(project) {
            ensemble(Symbol("Operations")) { "mathematics.Operations*" }
            ensemble(Symbol("Number")) { "mathematics.Number*" }
            ensemble(Symbol("Rational")) { "mathematics.Rational*" }
            ensemble(Symbol("Mathematics")) { "mathematics.Mathematics*" }
            ensemble(Symbol("Example")) { "mathematics.Example*" }

            Symbol("Example") is_only_allowed_to (USE, Symbol("Mathematics"))
        }

        val result = specification.analyze().map(_.toString).toSeq.sorted.mkString("\n")
        result should be(empty) // <= primary test

        testEnsemblesAreNonEmpty(specification)
    }

    it should ("correctly identify deviations between the specified (using is_only_allowed_to_use constraints) and implemented architecture") in {
        val specification = new Specification(project) {
            ensemble(Symbol("Operations")) { "mathematics.Operations*" }
            ensemble(Symbol("Number")) { "mathematics.Number*" }
            ensemble(Symbol("Rational")) { "mathematics.Rational*" }
            ensemble(Symbol("Mathematics")) { "mathematics.Mathematics*" }
            ensemble(Symbol("Example")) { "mathematics.Example*" }

            Symbol("Mathematics") is_only_allowed_to (USE, Symbol("Rational"))
        }
        specification.analyze() should not be (empty) // <= primary test

        testEnsemblesAreNonEmpty(specification)
    }

    /*
     * outgoing is_not_allowed_to constraint validation
     */
    it should ("validate the is_not_allowed_to_use constraint with no violations") in {
        val specification = new Specification(project) {
            ensemble(Symbol("Operations")) { "mathematics.Operations*" }
            ensemble(Symbol("Number")) { "mathematics.Number*" }
            ensemble(Symbol("Rational")) { "mathematics.Rational*" }
            ensemble(Symbol("Mathematics")) { "mathematics.Mathematics*" }
            ensemble(Symbol("Example")) { "mathematics.Example*" }

            Symbol("Example") is_not_allowed_to (USE, Symbol("Rational"))
        }
        val result = specification.analyze().map(_.toString).toSeq.sorted.mkString("\n")
        result should be(empty)

        testEnsemblesAreNonEmpty(specification)
    }

    it should ("validate the is_not_allowed_to_use constraint with violations") in {
        val specification = new Specification(project) {
            ensemble(Symbol("Operations")) { "mathematics.Operations*" }
            ensemble(Symbol("Number")) { "mathematics.Number*" }
            ensemble(Symbol("Rational")) { "mathematics.Rational*" }
            ensemble(Symbol("Mathematics")) { "mathematics.Mathematics*" }
            ensemble(Symbol("Example")) { "mathematics.Example*" }

            Symbol("Mathematics") is_not_allowed_to (USE, Symbol("Number"))
        }
        specification.analyze() should not be (empty)

        testEnsemblesAreNonEmpty(specification)
    }

    /*
     * incoming is_only_to_be_used_by constraint validation
     */
    it should ("validate the is_only_to_be_used_by constraint with no violations") in {
        val specification = new Specification(project) {
            ensemble(Symbol("Operations")) { "mathematics.Operations*" }
            ensemble(Symbol("Number")) { "mathematics.Number*" }
            ensemble(Symbol("Rational")) { "mathematics.Rational*" }
            ensemble(Symbol("Mathematics")) { "mathematics.Mathematics*" }
            ensemble(Symbol("Example")) { "mathematics.Example*" }

            Symbol("Mathematics") is_only_to_be_used_by Symbol("Example")
        }
        val result = specification.analyze().map(_.toString).toSeq.sorted.mkString("\n")
        result should be(empty)

        testEnsemblesAreNonEmpty(specification)
    }

    it should ("validate the is_only_to_be_used_by constraint with violations") in {
        val specification = new Specification(project) {
            ensemble(Symbol("Operations")) { "mathematics.Operations*" }
            ensemble(Symbol("Number")) { "mathematics.Number*" }
            ensemble(Symbol("Rational")) { "mathematics.Rational*" }
            ensemble(Symbol("Mathematics")) { "mathematics.Mathematics*" }
            ensemble(Symbol("Example")) { "mathematics.Example*" }

            Symbol("Rational") is_only_to_be_used_by Symbol("Mathematics")
        }
        specification.analyze() should not be (empty)

        testEnsemblesAreNonEmpty(specification)
    }

    /*
     * incoming allows_incoming_dependencies_from constraint validation
     */
    it should ("validate the allows_incoming_dependencies_from constraint with no violations") in {
        val specification = new Specification(project) {
            ensemble(Symbol("Operations")) { "mathematics.Operations*" }
            ensemble(Symbol("Number")) { "mathematics.Number*" }
            ensemble(Symbol("Rational")) { "mathematics.Rational*" }
            ensemble(Symbol("Mathematics")) { "mathematics.Mathematics*" }
            ensemble(Symbol("Example")) { "mathematics.Example*" }

            Symbol("Mathematics") allows_incoming_dependencies_from Symbol("Example")
        }
        val result = specification.analyze().map(_.toString).toSeq.sorted.mkString("\n")
        result should be(empty)

        testEnsemblesAreNonEmpty(specification)
    }

    it should ("validate the allows_incoming_dependencies_from constraint with violations") in {
        val specification = new Specification(project) {
            ensemble(Symbol("Operations")) { "mathematics.Operations*" }
            ensemble(Symbol("Number")) { "mathematics.Number*" }
            ensemble(Symbol("Rational")) { "mathematics.Rational*" }
            ensemble(Symbol("Mathematics")) { "mathematics.Mathematics*" }
            ensemble(Symbol("Example")) { "mathematics.Example*" }

            Symbol("Number") allows_incoming_dependencies_from Symbol("Rational")
        }
        specification.analyze() should not be (empty)

        testEnsemblesAreNonEmpty(specification)
    }

    /*
     * outgoing every_element_should_implement_method constraint
     */
    it should ("validate the every_element_should_implement_method constraint with no violations") in {
        val specification = new Specification(project) {
            ensemble(Symbol("Mathematics"))(ClassMatcher(
                "mathematics.Mathematics",
                matchPrefix = false, matchMethods = false, matchFields = false
            ))

            Symbol("Mathematics") every_element_should_implement_method MethodWithName("operation1")

        }
        specification.analyze() should be(empty)

        testEnsemblesAreNonEmpty(specification)
    }

    it should ("validate the every_element_should_implement_method constraint with violations") in {
        val specification = new Specification(project) {
            ensemble(Symbol("Operations"))(ClassMatcher(
                "mathematics.Operations",
                matchPrefix = false, matchMethods = false, matchFields = false
            ))

            Symbol("Operations") every_element_should_implement_method MethodWithName("operation1")
        }
        specification.analyze() should not be (empty)

        testEnsemblesAreNonEmpty(specification)
    }

    /*
     * outgoing every_element_should_extend constraint
     */
    it should ("validate the every_element_should_extend constraint with no violations") in {
        val project = ClassFiles(locateTestResources("classfiles/entity.jar", "av"))
        val specification = new Specification(project) {
            ensemble(Symbol("Address"))(ClassMatcher(
                "entity.impl.Address",
                matchPrefix = false, matchMethods = false, matchFields = false
            ))

            ensemble(Symbol("User"))(ClassMatcher(
                "entity.impl.User",
                matchPrefix = false, matchMethods = false, matchFields = false
            ))

            ensemble(Symbol("AbstractEntity"))(ClassMatcher(
                "entity.AbstractEntity",
                matchPrefix = false, matchMethods = false, matchFields = false
            ))

            Symbol("Address") every_element_should_extend Symbol("AbstractEntity")

        }

        specification.analyze() should be(empty)

        testEnsemblesAreNonEmpty(specification)
    }

    it should ("validate the every_element_should_extend constraint with violations") in {
        val project = ClassFiles(locateTestResources("classfiles/entity.jar", "av"))
        val specification = new Specification(project) {
            ensemble(Symbol("Address"))(ClassMatcher(
                "entity.impl.Address",
                matchPrefix = false, matchMethods = false, matchFields = false
            ))

            ensemble(Symbol("User"))(ClassMatcher(
                "entity.impl.User",
                matchPrefix = false, matchMethods = false, matchFields = false
            ))

            ensemble(Symbol("AbstractEntity"))(ClassMatcher(
                "entity.AbstractEntity",
                matchPrefix = false, matchMethods = false, matchFields = false
            ))

            Symbol("Address") every_element_should_extend Symbol("User")

        }

        specification.analyze() should not be (empty)

        testEnsemblesAreNonEmpty(specification)
    }

    /*
     * outgoing every_element_should_be_annotated_with constraint
     */
    it should ("validate every_element_should_be_annotated_with constraint with no violations") in {
        val project = ClassFiles(locateTestResources("classfiles/entity.jar", "av"))
        val specification = new Specification(project) {

            ensemble(Symbol("EntityId"))(FieldMatcher(AllClasses, theName = Some("id")))

            Symbol("EntityId") every_element_should_be_annotated_with (AnnotatedWith("entity.annotation.Id"))
        }

        specification.analyze() should be(empty)

        testEnsemblesAreNonEmpty(specification)

    }

    it should ("validate every_element_should_be_annotated_with constraint with violations") in {
        val project = ClassFiles(locateTestResources("classfiles/entity.jar", "av"))
        val specification = new Specification(project) {

            ensemble(Symbol("EntityId"))(FieldMatcher(AllClasses, theName = Some("id")))

            Symbol("EntityId") every_element_should_be_annotated_with (AnnotatedWith("entity.annotation.Entity"))
        }

        specification.analyze() should not be (empty)

        testEnsemblesAreNonEmpty(specification)

    }

    /*
     * outgoing every_element_should_be_annotated_with (multiple annotations) constraint
     */
    it should ("validate every_element_should_be_annotated_with (multiple annotations) constraint with no violations") in {
        val project = ClassFiles(locateTestResources("classfiles/entity.jar", "av"))
        val specification = new Specification(project) {

            ensemble(Symbol("EntityId"))(FieldMatcher(AllClasses, theName = Some("id")))

            Symbol("EntityId") every_element_should_be_annotated_with
                ("(entity.annotation.Id - entity.annotation.Column)",
                    Seq(
                        AnnotatedWith("entity.annotation.Id"),
                        AnnotatedWith(
                            "entity.annotation.Column",
                            "name" -> StringValue("id"), "nullable" -> BooleanValue(false)
                        )
                    )
                )
        }

        specification.analyze() should be(empty)

        testEnsemblesAreNonEmpty(specification)

    }

    it should ("validate every_element_should_be_annotated_with (multiple annotations) constraint with violations") in {
        val project = ClassFiles(locateTestResources("classfiles/entity.jar", "av"))
        val specification = new Specification(project) {

            ensemble(Symbol("EntityId"))(FieldMatcher(AllClasses, theName = Some("id")))

            Symbol("EntityId") every_element_should_be_annotated_with
                ("(entity.annotation.Id - entity.annotation.Column)",
                    Seq(
                        AnnotatedWith("entity.annotation.Id"),
                        AnnotatedWith(
                            "entity.annotation.Column",
                            "name" -> StringValue("id"), "nullable" -> BooleanValue(true)
                        )
                    )
                )
        }

        specification.analyze() should not be (empty)

        testEnsemblesAreNonEmpty(specification)

    }

}
