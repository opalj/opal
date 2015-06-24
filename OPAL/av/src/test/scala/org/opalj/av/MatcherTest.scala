/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj.av
package checking

import org.junit.runner.RunWith
import org.opalj.bi.TestSupport.locateTestResources
import org.opalj.br.reader.Java8Framework.ClassFiles
import org.opalj.br.analyses.Project
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.opalj.br.StringValue
import org.opalj.br.BooleanValue
import org.opalj.br.Annotation
import org.opalj.br.FieldType
import org.opalj.br.ObjectType
import scala.collection.mutable.ArrayBuffer
import org.opalj.br.ElementValuePair
import org.opalj.bi.AccessFlagsMatcher
import scala.collection.IndexedSeq

/**
 * Tests matchers of the Architecture Validation Framework.
 *
 * @author Marco Torsello
 */
@RunWith(classOf[JUnitRunner])
class MatcherTest extends FunSuite with Matchers with BeforeAndAfterAll {

    val project = Project(ClassFiles(locateTestResources("classfiles/entity.jar", "av")))

    /*
     * SimpleNamePredicate
     */
    test("the SimpleNamePredicate should match the given name") {
        val matched1 = SimpleNamePredicate("entity/impl/User")("entity/impl/User")
        matched1 should be(true)

        val matched2 = SimpleNamePredicate("entity", true)("entity/impl/User")
        matched2 should be(true)

        val matched3 = SimpleNamePredicate("entity/impl/User", false)("entity/impl/User")
        matched3 should be(true)
    }

    test("the SimpleNamePredicate should not match the given name") {
        val matched1 = SimpleNamePredicate("entity/impl/Contact")("entity/impl/User")
        matched1 should be(false)

        val matched2 = SimpleNamePredicate("entity/impl/C", true)("entity/impl/User")
        matched2 should be(false)

        val matched3 = SimpleNamePredicate("entity/impl/Contact", false)("entity/impl/User")
        matched3 should be(false)
    }

    /*
     * RegexNamePredicate
     */
    test("the RegexNameMatcher should match the given name") {
        val matched1 = RegexNamePredicate(""".+User""".r)("entity/impl/User")
        matched1 should be(true)
    }

    test("the RegexNamePredicate should not match the given name") {
        val matched1 = RegexNamePredicate(""".+Contact""".r)("entity/impl/User")
        matched1 should be(false)
    }

    /*
     * AnnotationPredicate
     */
    test("the AnnotationPredicate should match the given annotation") {
        val matched1 = AnnotationPredicate("entity.annotation.Transient")(
            Annotation(ObjectType("entity/annotation/Transient"), IndexedSeq.empty))
        matched1 should be(true)

        val matched2 = AnnotationPredicate("entity.annotation.Column",
            Map("name" -> StringValue("first_name")))(
                Annotation(ObjectType("entity/annotation/Column"),
                    ArrayBuffer[ElementValuePair](ElementValuePair("name", StringValue("first_name")))))
        matched2 should be(true)
    }

    test("the AnnotationPredicate should not match the given annotation") {
        val matched1 = AnnotationPredicate("entity.annotation.Column")(
            Annotation(ObjectType("entity/annotation/Transient"), IndexedSeq.empty))
        matched1 should be(false)

        val matched2 = AnnotationPredicate("entity.annotation.Column",
            Map("name" -> StringValue("first_name")))(
                Annotation(ObjectType("entity/annotation/Transient"),
                    ArrayBuffer[ElementValuePair](ElementValuePair("name", StringValue("last_name")))))
        matched2 should be(false)
    }

    /*
     * AnnotationsPredicate
     */
    test("the AnnotationsPredicate should match the given annotations") {
        val matched1 = StrictlyAllAnnotationsPredicate(Set(AnnotationPredicate("entity.annotation.Transient")))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Transient"), IndexedSeq.empty)))
        matched1 should be(true)

        val matched2 = StrictlyAllAnnotationsPredicate(Set(AnnotationPredicate("entity.annotation.Column",
            Map("name" -> StringValue("first_name")))))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Column"),
                ArrayBuffer[ElementValuePair](ElementValuePair("name", StringValue("first_name"))))))
        matched2 should be(true)

        val matched3 = AllAnnotationsPredicate(Set(AnnotationPredicate("entity.annotation.Transient")))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Transient"), IndexedSeq.empty)))
        matched3 should be(true)

        val matched4 = AllAnnotationsPredicate(Set(AnnotationPredicate("entity.annotation.Column",
            Map("name" -> StringValue("first_name")))))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Column"),
                ArrayBuffer[ElementValuePair](ElementValuePair("name", StringValue("first_name"))))))
        matched4 should be(true)

        val matched5 = AllAnnotationsPredicate(Set(AnnotationPredicate("entity.annotation.Column")))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Transient"), IndexedSeq.empty),
                Annotation(ObjectType("entity/annotation/Column"), IndexedSeq.empty)))
        matched5 should be(true)

        val matched6 = AllAnnotationsPredicate(Set(AnnotationPredicate("entity.annotation.Column"),
            AnnotationPredicate("entity.annotation.Transient")))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Transient"), IndexedSeq.empty),
                Annotation(ObjectType("entity/annotation/Column"), IndexedSeq.empty)))
        matched6 should be(true)

        val matched7 = AnyAnnotationsPredicate(Set(AnnotationPredicate("entity.annotation.Column"),
            AnnotationPredicate("entity.annotation.Transient")))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Transient"), IndexedSeq.empty)))
        matched7 should be(true)
    }

    test("the AnnotationsPredicate should not match the given annotations") {
        val matched1 = StrictlyAllAnnotationsPredicate(Set(AnnotationPredicate("entity.annotation.Column")))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Transient"), IndexedSeq.empty)))
        matched1 should be(false)

        val matched2 = StrictlyAllAnnotationsPredicate(Set(AnnotationPredicate("entity.annotation.Column",
            Map("name" -> StringValue("first_name")))))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Transient"),
                ArrayBuffer[ElementValuePair](ElementValuePair("name", StringValue("last_name"))))))
        matched2 should be(false)

        val matched3 = StrictlyAllAnnotationsPredicate(Set(AnnotationPredicate("entity.annotation.Column")))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Transient"), IndexedSeq.empty),
                Annotation(ObjectType("entity/annotation/Column"), IndexedSeq.empty)))
        matched3 should be(false)

        val matched4 = StrictlyAllAnnotationsPredicate(Set(AnnotationPredicate("entity.annotation.Column"),
            AnnotationPredicate("entity.annotation.Transient")))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Transient"), IndexedSeq.empty)))
        matched4 should be(false)

        val matched5 = AllAnnotationsPredicate(Set(AnnotationPredicate("entity.annotation.Column"),
            AnnotationPredicate("entity.annotation.Transient")))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Transient"), IndexedSeq.empty)))
        matched5 should be(false)

        val matched6 = AnyAnnotationsPredicate(Set(AnnotationPredicate("entity.annotation.Column"),
            AnnotationPredicate("entity.annotation.Transient")))(
            IndexedSeq(Annotation(ObjectType("entity/annotation/Embedded"), IndexedSeq.empty)))
        matched6 should be(false)
    }

    /*
     * ClassMatcher matching classes only
     */
    test("the ClassMatcher should match only classes") {
        val matchedClasses1 = DefaultClassMatcher(namePredicate = SimpleNamePredicate("entity.impl.User"), matchMethods = false, matchFields = false).extension(project)
        matchedClasses1 should not be (empty)
        matchedClasses1.size should be(1)

        val matchedClasses2 = DefaultClassMatcher(namePredicate = SimpleNamePredicate("entity/impl/User", false), matchMethods = false, matchFields = false).extension(project)
        matchedClasses2 should not be (empty)
        matchedClasses2.size should be(1)

        val matchedClasses3 = DefaultClassMatcher(namePredicate = SimpleNamePredicate("entity", true), matchMethods = false, matchFields = false).extension(project)
        matchedClasses3 should not be (empty)
        matchedClasses3.size should be(8)

        val matchedClasses4 = DefaultClassMatcher(namePredicate = RegexNamePredicate(""".+User""".r), matchMethods = false, matchFields = false).extension(project)
        matchedClasses4 should not be (empty)
        matchedClasses4.size should be(1)
    }

    test("the ClassMatcher should not match any class") {
        val matchedClasses1 = ClassMatcher("entity.impl.Contact").extension(project)
        matchedClasses1 should be(empty)

        val matchedClasses2 = ClassMatcher(SimpleNamePredicate("entity/impl/Contact", false)).extension(project)
        matchedClasses2 should be(empty)

        val matchedClasses3 = ClassMatcher(SimpleNamePredicate("entity/impl/C", true)).extension(project)
        matchedClasses3 should be(empty)

        val matchedClasses4 = ClassMatcher(RegexNamePredicate(""".+Contact""".r)).extension(project)
        matchedClasses4 should be(empty)
    }

    /*
     * ClassMatcher with AccessFlags matching classes only
     */
    test("the ClassMatcher should match only classes with the given access flags") {
        val matchedClasses1 = DefaultClassMatcher(AccessFlagsMatcher.NOT_ABSTRACT, matchMethods = false, matchFields = false).extension(project)
        matchedClasses1 should not be (empty)
        matchedClasses1.size should be(2)

        val matchedClasses2 = DefaultClassMatcher(AccessFlagsMatcher.PUBLIC_ABSTRACT, matchMethods = false, matchFields = false).extension(project)
        matchedClasses2 should not be (empty)
        matchedClasses2.size should be(6)

        val matchedClasses3 = DefaultClassMatcher(AccessFlagsMatcher.PUBLIC, matchMethods = false, matchFields = false).extension(project)
        matchedClasses3 should not be (empty)
        matchedClasses3.size should be(8)

        val matchedClasses4 = DefaultClassMatcher(AccessFlagsMatcher.ALL, matchMethods = false, matchFields = false).extension(project)
        matchedClasses4 should not be (empty)
        matchedClasses4.size should be(8)

        val matchedClasses5 = DefaultClassMatcher(AccessFlagsMatcher.PUBLIC && AccessFlagsMatcher.NOT_ABSTRACT, matchMethods = false, matchFields = false).extension(project)
        matchedClasses5 should not be (empty)
        matchedClasses5.size should be(2)
    }

    test("the ClassMatcher should not match any class with the given access flags") {
        val matchedClasses1 = DefaultClassMatcher(AccessFlagsMatcher.PRIVATE_FINAL, matchMethods = false, matchFields = false).extension(project)
        matchedClasses1 should be(empty)

        val matchedClasses3 = DefaultClassMatcher(AccessFlagsMatcher.PUBLIC_STATIC_FINAL, matchMethods = false, matchFields = false).extension(project)
        matchedClasses3 should be(empty)

        val matchedClasses4 = DefaultClassMatcher(AccessFlagsMatcher.PUBLIC_STATIC, matchMethods = false, matchFields = false).extension(project)
        matchedClasses4 should be(empty)
    }

    /*
     * ClassMatcher with Annotation matching classes only
     */
    test("the ClassMatcher should match only classes with the given annotation") {
        val matchedClasses1 = DefaultClassMatcher(annotationsPredicate = AllAnnotationsPredicate(Set(AnnotationPredicate("entity.annotation.Entity"))), matchMethods = false, matchFields = false).extension(project)
        matchedClasses1 should not be (empty)
        matchedClasses1.size should be(2)
    }

    test("the ClassMatcher should not match any class with the given annotation") {
        val matchedClasses1 = DefaultClassMatcher(annotationsPredicate = AllAnnotationsPredicate(Set(AnnotationPredicate("entity.annotation.Transient"))), matchMethods = false, matchFields = false).extension(project)
        matchedClasses1 should be(empty)
    }

    /*
     * ClassMatcher matching complete classes
     */
    test("the ClassMatcher should match any element of the class") {
        val matchedClasses1 = ClassMatcher("entity.impl.User").extension(project)
        matchedClasses1 should not be (empty)
        matchedClasses1.size should be(19)

        val matchedClasses2 = ClassMatcher(SimpleNamePredicate("entity/impl/User", false)).extension(project)
        matchedClasses2 should not be (empty)
        matchedClasses2.size should be(19)

        val matchedClasses3 = ClassMatcher(SimpleNamePredicate("entity", true)).extension(project)
        matchedClasses3 should not be (empty)
        matchedClasses3.size should be(46)

        val matchedClasses4 = ClassMatcher(RegexNamePredicate(""".+User""".r)).extension(project)
        matchedClasses4 should not be (empty)
        matchedClasses4.size should be(19)
    }

    test("the ClassMatcher should not match any element") {
        val matchedClasses1 = ClassMatcher("entity.impl.Contact").extension(project)
        matchedClasses1 should be(empty)

        val matchedClasses2 = ClassMatcher(SimpleNamePredicate("entity/impl/Contact", false)).extension(project)
        matchedClasses2 should be(empty)

        val matchedClasses3 = ClassMatcher(SimpleNamePredicate("entity/impl/C", true)).extension(project)
        matchedClasses3 should be(empty)

        val matchedClasses4 = ClassMatcher(RegexNamePredicate(""".+Contact""".r)).extension(project)
        matchedClasses4 should be(empty)
    }

    /*
     * ClassMatcher with Annotation matching complete classes
     */
    test("the ClassMatcher should match any element of the classes annotated with the given annotation") {
        val matchedClasses1 = ClassMatcher(AllAnnotationsPredicate(
            Set(AnnotationPredicate("entity.annotation.Entity")))).extension(project)
        matchedClasses1 should not be (empty)
        matchedClasses1.size should be(34)
    }

    test("the ClassMatcher should not match any element with the given annotation") {
        val matchedClasses1 = ClassMatcher(AllAnnotationsPredicate(
            Set(AnnotationPredicate("entity.annotation.Transient")))).extension(project)
        matchedClasses1 should be(empty)
    }

    /*
     * PackageMatcher matching classes only
     */
    test("the PackageMatcher should match only classes") {
        val classMatcher = DefaultClassMatcher(matchMethods = false, matchFields = false)

        val matchedClasses1 = PackageMatcher("entity", classMatcher).extension(project)
        matchedClasses1 should not be (empty)
        matchedClasses1.size should be(1)

        val matchedClasses2 = PackageMatcher("entity", classMatcher, true).extension(project)
        matchedClasses2 should not be (empty)
        matchedClasses2.size should be(8)

        val matchedClasses3 = PackageMatcher(SimpleNamePredicate("entity", false), classMatcher).extension(project)
        matchedClasses3 should not be (empty)
        matchedClasses3.size should be(1)

        val matchedClasses4 = PackageMatcher(RegexNamePredicate(""".+impl""".r), classMatcher).extension(project)
        matchedClasses4 should not be (empty)
        matchedClasses4.size should be(2)
    }

    test("the PackageMatcher should not match any class") {
        val classMatcher = DefaultClassMatcher(matchMethods = false, matchFields = false)

        val matchedClasses1 = PackageMatcher("entity.user", classMatcher).extension(project)
        matchedClasses1 should be(empty)

        val matchedClasses2 = PackageMatcher(SimpleNamePredicate("entity/user", false), classMatcher).extension(project)
        matchedClasses2 should be(empty)

        val matchedClasses3 = PackageMatcher(SimpleNamePredicate("entity/u", true), classMatcher).extension(project)
        matchedClasses3 should be(empty)

        val matchedClasses4 = PackageMatcher(RegexNamePredicate(""".+user""".r), classMatcher).extension(project)
        matchedClasses4 should be(empty)
    }

    /*
         * PackageMatcher matching complete classes
         */
    test("the PackageMatcher should match all elements of the class") {
        val matchedClasses1 = PackageMatcher("entity").extension(project)
        matchedClasses1 should not be (empty)
        matchedClasses1.size should be(3)

        val matchedClasses2 = PackageMatcher("entity", true).extension(project)
        matchedClasses2 should not be (empty)
        matchedClasses2.size should be(46)

        val matchedClasses3 = PackageMatcher(SimpleNamePredicate("entity", false)).extension(project)
        matchedClasses3 should not be (empty)
        matchedClasses3.size should be(3)

        val matchedClasses4 = PackageMatcher(RegexNamePredicate(""".+impl""".r)).extension(project)
        matchedClasses4 should not be (empty)
        matchedClasses4.size should be(34)
    }

    test("the PackageMatcher should not match any element") {
        val matchedClasses1 = PackageMatcher("entity.user").extension(project)
        matchedClasses1 should be(empty)

        val matchedClasses2 = PackageMatcher(SimpleNamePredicate("entity/user", false)).extension(project)
        matchedClasses2 should be(empty)

        val matchedClasses3 = PackageMatcher(SimpleNamePredicate("entity/u", true)).extension(project)
        matchedClasses3 should be(empty)

        val matchedClasses4 = PackageMatcher(RegexNamePredicate(""".+user""".r)).extension(project)
        matchedClasses4 should be(empty)
    }

    /*
     * FieldMatcher
     */
    test("the FieldMatcher should match fields of the class") {
        val matchedFields1 = FieldMatcher(ClassMatcher("entity.impl.User")).extension(project)
        matchedFields1 should not be (empty)
        matchedFields1.size should be(6)

        val matchedFields2 = FieldMatcher(
            ClassMatcher("entity.impl.User"),
            AllAnnotationsPredicate(Set(AnnotationPredicate("entity.annotation.Column", Map("name" -> StringValue("first_name"), "nullable" -> BooleanValue(false)))))).extension(project)
        matchedFields2 should not be (empty)
        matchedFields2.size should be(1)

        val matchedFields3 = FieldMatcher(ClassMatcher("entity.impl.User"), fieldType = Some("Ljava.lang.String;")).extension(project)
        matchedFields3 should not be (empty)
        matchedFields3.size should be(3)

        val matchedFields4 = FieldMatcher(ClassMatcher("entity.impl.User"), fieldName = Some("firstName")).extension(project)
        matchedFields4 should not be (empty)
        matchedFields4.size should be(1)
    }

    test("the FieldMatcher should not match any element") {
        val matchedFields1 = FieldMatcher(ClassMatcher("entity.impl.Contact")).extension(project)
        matchedFields1 should be(empty)

        val matchedFields2 = FieldMatcher(
            ClassMatcher("entity.impl.User"),
            AllAnnotationsPredicate(Set(AnnotationPredicate("entity.annotation.Column", Map("name" -> StringValue("street"), "nullable" -> BooleanValue(false)))))).extension(project)
        matchedFields2 should be(empty)

        val matchedFields3 = FieldMatcher(ClassMatcher("entity.impl.User"), fieldType = Some("Ljava.lang.Integer;")).extension(project)
        matchedFields3 should be(empty)

        val matchedFields4 = FieldMatcher(ClassMatcher("entity.impl.User"), fieldName = Some("street")).extension(project)
        matchedFields4 should be(empty)
    }

    /*
     * MethodMatcher
     */
    test("the MethodMatcher should match methods of the class") {
        val matchedMethods1 = MethodMatcher(ClassMatcher("entity.impl.User")).extension(project)
        matchedMethods1 should not be (empty)
        matchedMethods1.size should be(12)

        val matchedMethods2 = MethodMatcher(
            ClassMatcher("entity.impl.User"),
            AnnotationPredicate("entity.annotation.Transient")).extension(project)
        matchedMethods2 should not be (empty)
        matchedMethods2.size should be(1)

        val matchedMethods3 = MethodMatcher(ClassMatcher("entity.impl.User"), MethodPredicate("getFullName")).extension(project)
        matchedMethods3 should not be (empty)
        matchedMethods3.size should be(1)
    }

    test("the MethodMatcher should not match any element") {
        val matchedFields1 = MethodMatcher(ClassMatcher("entity.impl.Contact")).extension(project)
        matchedFields1 should be(empty)

        val matchedFields2 = MethodMatcher(
            ClassMatcher("entity.impl.User"),
            AnnotationPredicate("entity.annotation.Column", Map("name" -> StringValue("street"), "nullable" -> BooleanValue(false)))).extension(project)
        matchedFields2 should be(empty)

        val matchedFields3 = MethodMatcher(ClassMatcher("entity.impl.User"), MethodPredicate("getStreet")).extension(project)
        matchedFields3 should be(empty)
    }
}
