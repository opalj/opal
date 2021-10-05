/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

import org.junit.runner.RunWith

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.junit.JUnitRunner

import scala.collection.IndexedSeq

import org.opalj.bi.TestResources.locateTestResources
import org.opalj.bi.AccessFlagsMatcher._
import org.opalj.br.reader.Java8Framework.ClassFiles
import org.opalj.br.analyses.Project
import org.opalj.br.StringValue
import org.opalj.br.BooleanValue
import org.opalj.br.Annotation
import org.opalj.br.ObjectType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ElementValuePair
import org.opalj.br.ElementValuePairs
import org.opalj.br.NoElementValuePairs

/**
 * Tests matchers of the Architecture Validation Framework.
 *
 * @author Marco Torsello
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class MatcherTest extends AnyFunSuite with Matchers with BeforeAndAfterAll {

    val project = Project(ClassFiles(locateTestResources("classfiles/entity.jar", "av")))

    /*
     * SimpleNamePredicate
     */
    test("the SimpleNamePredicate should match the given name") {

        Equals("entity/impl/User")("entity/impl/User") should be(true)

        StartsWith("entity")("entity/impl/User") should be(true)

        Equals("entity/impl/User")("entity/impl/User") should be(true)
    }

    test("the SimpleNamePredicate should not match the given name") {

        Equals("entity/impl/Contact")("entity/impl/User") should be(false)

        StartsWith("entity/impl/C")("entity/impl/User") should be(false)

        Equals("impl/Contact")("entity/impl/Contact") should be(false)
    }

    /*
     * MethodPredicate
     */
    test("the simple MethodPredicates") {
        MethodMatcher(
            ClassMatcher("entity.impl.User"), MethodWithName("lastName")
        ).extension(project).size should be(1)

        MethodMatcher(
            ClassMatcher("entity.impl.User"),
            MethodWithSignature("lastName", MethodDescriptor.JustReturnsString)
        ).extension(project).size should be(1)

        MethodMatcher(
            ClassMatcher("entity.impl.User"),
            MethodWithSignature("lastName", MethodDescriptor.JustReturnsBoolean)
        ).extension(project).size should be(0)
    }

    test("the combination of simple (Method)Predicates") {
        MethodMatcher(
            ClassMatcher("entity.impl.User"),
            MethodWithName("lastName") and AccessFlags(PUBLIC)
        ).extension(project).size should be(1)

        MethodMatcher(
            ClassMatcher("entity.impl.User"),
            MethodWithSignature("lastName", MethodDescriptor.JustReturnsString) and
                AccessFlags(PUBLIC)
        ).extension(project).size should be(1)

        MethodMatcher(
            ClassMatcher("entity.impl.User"), AnyMethod having AccessFlags(STATIC)
        ).extension(project).size should be(0)
    }

    /*
     * RegexNamePredicate
     */
    test("the RegexNameMatcher should match the given name") {
        RegexNamePredicate(""".+User""".r)("entity/impl/User") should be(true)
    }

    test("the RegexNamePredicate should not match the given name") {
        RegexNamePredicate(""".+Contact""".r)("entity/impl/User") should be(false)
    }

    /*
     * AnnotatedWith
     */
    test("that AnnotatedWith perform a precise match of the given annotation") {
        AnnotatedWith("entity.annotation.Transient")(
            Annotation(ObjectType("entity/annotation/Transient"))
        ) should be(true)

        AnnotatedWith("entity.annotation.Column", "name" -> StringValue("first_name"))(
            Annotation(
                ObjectType("entity/annotation/Column"),
                ElementValuePairs(ElementValuePair("name", StringValue("first_name")))
            )
        ) should be(true)
    }

    test("the AnnotatedWith should not match the given annotation") {
        AnnotatedWith("entity.annotation.Column")(
            Annotation(ObjectType("entity/annotation/Transient"))
        ) should be(false)

        val fixture =
            Annotation(
                ObjectType("entity/annotation/Transient"),
                ElementValuePair("name", StringValue("last_name"))
            )
        AnnotatedWith(
            "entity.annotation.Column",
            Map("name" -> StringValue("first_name"))
        )(fixture) should be(false)
    }

    /*
     * AnnotationsPredicate
     */
    test("the AnnotationsPredicate should match the given annotations") {

        HasAtLeastTheAnnotations(AnnotatedWith("entity.annotation.Transient"))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Transient"), NoElementValuePairs))
        ) should be(true)

        val columnAnnotation = IndexedSeq(
            Annotation(
                ObjectType("entity/annotation/Column"),
                ElementValuePair("name", StringValue("first_name"))
            )
        )
        HasAtLeastTheAnnotations(
            AnnotatedWith("entity.annotation.Column", "name" -> StringValue("first_name"))
        )(columnAnnotation) should be(true)

        HasAtLeastTheAnnotations(AnnotatedWith("entity.annotation.Transient"))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Transient"), NoElementValuePairs))
        ) should be(true)

        HasAtLeastTheAnnotations(AnnotatedWith("entity.annotation.Column", "name" -> StringValue("first_name")))(
            IndexedSeq(Annotation(
                ObjectType("entity/annotation/Column"),
                ElementValuePairs(ElementValuePair("name", StringValue("first_name")))
            ))
        ) should be(true)

        HasAtLeastTheAnnotations(AnnotatedWith("entity.annotation.Column"))(
            IndexedSeq(
                Annotation(ObjectType("entity/annotation/Transient"), NoElementValuePairs),
                Annotation(ObjectType("entity/annotation/Column"), NoElementValuePairs)
            )
        ) should be(true)

        HasAtLeastTheAnnotations(Set(
            AnnotatedWith("entity.annotation.Column"),
            AnnotatedWith("entity.annotation.Transient")
        ))(
            IndexedSeq(
                Annotation(ObjectType("entity/annotation/Transient"), NoElementValuePairs),
                Annotation(ObjectType("entity/annotation/Column"), NoElementValuePairs)
            )
        ) should be(true)

        HasAtLeastOneAnnotation(Set(
            AnnotatedWith("entity.annotation.Column"),
            AnnotatedWith("entity.annotation.Transient")
        ))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Transient"), NoElementValuePairs))
        ) should be(true)
    }

    test("the AnnotationsPredicate should not match the given annotations") {
        HasAtLeastTheAnnotations(AnnotatedWith("entity.annotation.Column"))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Transient"), NoElementValuePairs))
        ) should be(false)

        HasAtLeastTheAnnotations(
            AnnotatedWith("entity.annotation.Column", Map("name" -> StringValue("first_name")))
        )(
                IndexedSeq(
                    Annotation(
                        ObjectType("entity/annotation/Transient"),
                        ElementValuePairs(ElementValuePair("name", StringValue("last_name")))
                    )
                )
            ) should be(false)

        HasTheAnnotations(AnnotatedWith("entity.annotation.Column"))(
            IndexedSeq(
                Annotation(ObjectType("entity/annotation/Transient"), NoElementValuePairs),
                Annotation(ObjectType("entity/annotation/Column"), NoElementValuePairs)
            )
        ) should be(false)

        HasAtLeastTheAnnotations(Set(
            AnnotatedWith("entity.annotation.Column"),
            AnnotatedWith("entity.annotation.Transient")
        ))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Transient"), NoElementValuePairs))
        ) should be(false)

        HasTheAnnotations(Set(
            AnnotatedWith("entity.annotation.Column"),
            AnnotatedWith("entity.annotation.Transient")
        ))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Transient"), NoElementValuePairs))
        ) should be(false)

        HasAtLeastOneAnnotation(Set(
            AnnotatedWith("entity.annotation.Column"),
            AnnotatedWith("entity.annotation.Transient")
        ))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Embedded"), NoElementValuePairs))
        ) should be(false)
    }

    /*
     * ClassMatcher matching classes only
     */
    test("the ClassMatcher should match only classes") {
        DefaultClassMatcher(
            namePredicate = Equals("entity.impl.User"),
            matchMethods = false, matchFields = false
        ).extension(project).size should be(1)

        DefaultClassMatcher(
            namePredicate = Equals("entity/impl/User"),
            matchMethods = false, matchFields = false
        ).extension(project).size should be(1)

        DefaultClassMatcher(
            namePredicate = StartsWith("entity"),
            matchMethods = false, matchFields = false
        ).extension(project).size should be(8)

        DefaultClassMatcher(
            namePredicate = RegexNamePredicate(""".+User""".r),
            matchMethods = false, matchFields = false
        ).extension(project).size should be(1)
    }

    test("the ClassMatcher should not match any class") {
        ClassMatcher("entity.impl.Contact").extension(project) should be(empty)

        ClassMatcher(Equals("entity/impl/Contact")).extension(project) should be(empty)

        ClassMatcher(StartsWith("entity/impl/C")).extension(project) should be(empty)

        ClassMatcher(RegexNamePredicate(""".+Contact""".r)).extension(project) should be(empty)
    }

    /*
     * ClassMatcher with AccessFlags matching classes only
     */
    test("the ClassMatcher should match only classes with the given access flags") {

        DefaultClassMatcher(
            NOT_ABSTRACT, matchMethods = false, matchFields = false
        ).extension(project).size should be(2)

        DefaultClassMatcher(
            PUBLIC_ABSTRACT, matchMethods = false, matchFields = false
        ).extension(project).size should be(6)

        DefaultClassMatcher(
            PUBLIC, matchMethods = false, matchFields = false
        ).extension(project).size should be(8)

        DefaultClassMatcher(
            ANY, matchMethods = false, matchFields = false
        ).extension(project).size should be(8)

        DefaultClassMatcher(
            PUBLIC && NOT_ABSTRACT, matchMethods = false, matchFields = false
        ).extension(project).size should be(2)
    }

    test("the ClassMatcher should not match any class with the given access flags") {
        DefaultClassMatcher(
            PRIVATE_FINAL, matchMethods = false, matchFields = false
        ).extension(project) should be(empty)

        DefaultClassMatcher(
            PUBLIC_STATIC_FINAL, matchMethods = false, matchFields = false
        ).extension(project) should be(empty)

        DefaultClassMatcher(
            PUBLIC_STATIC, matchMethods = false, matchFields = false
        ).extension(project) should be(empty)
    }

    /*
     * ClassMatcher with Annotation matching classes only
     */
    test("the ClassMatcher should match only classes with the given annotation") {

        DefaultClassMatcher(
            annotationsPredicate = HasAtLeastTheAnnotations(AnnotatedWith("entity.annotation.Entity")),
            matchMethods = false, matchFields = false
        ).extension(project).size should be(2)
    }

    test("the ClassMatcher should not match any class with the given annotation") {
        DefaultClassMatcher(
            annotationsPredicate = HasAtLeastTheAnnotations(AnnotatedWith("entity.annotation.Transient")),
            matchMethods = false, matchFields = false
        ).extension(project) should be(empty)
    }

    /*
     * ClassMatcher matching complete classes
     */
    test("the ClassMatcher should match any element of the class") {
        ClassMatcher("entity.impl.User").extension(project).size should be(19)

        ClassMatcher(Equals("entity/impl/User")).extension(project).size should be(19)

        ClassMatcher(StartsWith("entity")).extension(project).size should be(46)

        ClassMatcher(RegexNamePredicate(""".+User""".r)).extension(project).size should be(19)
    }

    test("the ClassMatcher should not match any element") {
        ClassMatcher("entity.impl.Contact").extension(project) should be(empty)

        ClassMatcher(Equals("entity/impl/Contact")).extension(project) should be(empty)

        ClassMatcher(StartsWith("entity/impl/C")).extension(project) should be(empty)

        ClassMatcher(RegexNamePredicate(""".+Contact""".r)).extension(project) should be(empty)
    }

    /*
     * ClassMatcher with Annotation matching complete classes
     */
    test("the ClassMatcher should match any element of the classes annotated with the given annotation") {

        ClassMatcher(
            HasAtLeastTheAnnotations(AnnotatedWith("entity.annotation.Entity"))
        ).extension(project).size should be(34)
    }

    test("the ClassMatcher should not match any element with the given annotation") {

        ClassMatcher(
            HasAtLeastTheAnnotations(AnnotatedWith("entity.annotation.Transient"))
        ).extension(project) should be(empty)
    }

    /*
     * PackageMatcher matching classes only
     */
    test("the PackageMatcher should match only classes") {
        val classMatcher = DefaultClassMatcher(matchMethods = false, matchFields = false)

        PackageMatcher("entity", classMatcher).extension(project).size should be(1)

        PackageMatcher("entity", classMatcher, true).extension(project).size should be(8)

        PackageMatcher(Equals("entity"), classMatcher).extension(project).size should be(1)

        PackageMatcher(
            RegexNamePredicate(""".+impl""".r), classMatcher
        ).extension(project).size should be(2)
    }

    test("the PackageMatcher should not match any class") {
        val classMatcher = DefaultClassMatcher(matchMethods = false, matchFields = false)

        PackageMatcher("entity.user", classMatcher).extension(project) should be(empty)

        PackageMatcher(Equals("entity/user"), classMatcher).extension(project) should be(empty)

        PackageMatcher(StartsWith("entity/u"), classMatcher).extension(project) should be(empty)

        PackageMatcher(
            RegexNamePredicate(""".+user""".r), classMatcher
        ).extension(project) should be(empty)
    }

    /*
         * PackageMatcher matching complete classes
         */
    test("the PackageMatcher should match all elements of the class") {
        PackageMatcher("entity").extension(project).size should be(3)

        PackageMatcher("entity", true).extension(project).size should be(46)

        PackageMatcher(Equals("entity")).extension(project).size should be(3)

        PackageMatcher(
            RegexNamePredicate(""".+impl""".r)
        ).extension(project).size should be(34)
    }

    test("the PackageMatcher should not match any element") {
        PackageMatcher("entity.user").extension(project) should be(empty)

        PackageMatcher(Equals("entity/user")).extension(project) should be(empty)

        PackageMatcher(StartsWith("entity/u")).extension(project) should be(empty)

        PackageMatcher(
            RegexNamePredicate(""".+user""".r)
        ).extension(project) should be(empty)
    }

    /*
     * FieldMatcher
     */
    test("the FieldMatcher should match fields of the class") {
        FieldMatcher(ClassMatcher("entity.impl.User"))(project).size should be(6)

        FieldMatcher(
            ClassMatcher("entity.impl.User"),
            HasAtLeastTheAnnotations(
                AnnotatedWith(
                    "entity.annotation.Column",
                    Map("name" -> StringValue("first_name"), "nullable" -> BooleanValue(false))
                )
            )
        ).extension(project).size should be(1)

        FieldMatcher(
            ClassMatcher("entity.impl.User"),
            theType = Some("Ljava.lang.String;")
        ).extension(project).size should be(3)

        FieldMatcher(
            ClassMatcher("entity.impl.User"),
            theName = Some("firstName")
        ).extension(project).size should be(1)
    }

    test("the FieldMatcher should not match any element") {

        FieldMatcher(ClassMatcher("entity.impl.Contact")).extension(project) should be(empty)

        FieldMatcher(
            ClassMatcher("entity.impl.User"),
            HasAtLeastTheAnnotations(
                AnnotatedWith(
                    "entity.annotation.Column",
                    "name" -> StringValue("street"),
                    "nullable" -> BooleanValue(false)
                )
            )
        ).extension(project) should be(empty)

        FieldMatcher(
            ClassMatcher("entity.impl.User"),
            theType = Some("Ljava.lang.Integer;")
        ).extension(project) should be(empty)

        FieldMatcher(
            ClassMatcher("entity.impl.User"),
            theName = Some("street")
        ).extension(project) should be(empty)
    }

    /*
     * MethodMatcher
     */
    test("the MethodMatcher should match methods of the class") {

        MethodMatcher(
            ClassMatcher("entity.impl.User")
        ).extension(project).size should be(12)

        MethodMatcher(
            ClassMatcher("entity.impl.User"), AnnotatedWith("entity.annotation.Transient")
        ).extension(project).size should be(1)

        MethodMatcher(
            ClassMatcher("entity.impl.User"), MethodWithName("getFullName")
        ).extension(project).size should be(1)
    }

    test("the MethodMatcher should not match any element") {
        MethodMatcher(
            ClassMatcher("entity.impl.Contact")
        ).extension(project) should be(empty)

        MethodMatcher(
            ClassMatcher("entity.impl.User"),
            AnnotatedWith(
                "entity.annotation.Column",
                "name" -> StringValue("street"), "nullable" -> BooleanValue(false)
            )
        ).extension(project) should be(empty)

        MethodMatcher(
            ClassMatcher("entity.impl.User"), MethodWithName("getStreet")
        ).extension(project) should be(empty)
    }

}
