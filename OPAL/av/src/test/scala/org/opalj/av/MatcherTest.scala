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

/**
 * Tests matchers of the Architecture Validation Framework.
 *
 * @author Marco Torsello
 */
@RunWith(classOf[JUnitRunner])
class MatcherTest extends FunSuite with Matchers with BeforeAndAfterAll {

    val project = Project(ClassFiles(locateTestResources("classfiles/entity.jar", "av")))

    /*
     * SimpleNameMatcher
     */
    test("the SimpleNameMatcher should match the given name") {
        val matched1 = SimpleNameMatcher("entity/impl/User").doesMatch("entity/impl/User")
        matched1 should be(true)

        val matched2 = SimpleNameMatcher("entity", true).doesMatch("entity/impl/User")
        matched2 should be(true)

        val matched3 = SimpleNameMatcher("entity/impl/User", false).doesMatch("entity/impl/User")
        matched3 should be(true)
    }

    test("the SimpleNameMatcher should not match the given name") {
        val matched1 = SimpleNameMatcher("entity/impl/Contact").doesMatch("entity/impl/User")
        matched1 should be(false)

        val matched2 = SimpleNameMatcher("entity/impl/C", true).doesMatch("entity/impl/User")
        matched2 should be(false)

        val matched3 = SimpleNameMatcher("entity/impl/Contact", false).doesMatch("entity/impl/User")
        matched3 should be(false)
    }

    /*
     * RegexNameMatcher
     */
    test("the RegexNameMatcher should match the given name") {
        val matched1 = RegexNameMatcher(""".+User""".r).doesMatch("entity/impl/User")
        matched1 should be(true)
    }

    test("the RegexNameMatcher should not match the given name") {
        val matched1 = RegexNameMatcher(""".+Contact""".r).doesMatch("entity/impl/User")
        matched1 should be(false)
    }

    /*
     * AnnotationMatcher
     */
    test("the AnnotationMatcher should match the given annotation") {
        val matched1 = AnnotationMatcher("javax.persistence.Transient").doesMatch(
            Annotation(ObjectType("javax/persistence/Transient"), IndexedSeq.empty))
        matched1 should be(true)

        val matched2 = AnnotationMatcher("javax.persistence.Column",
            Map("name" -> StringValue("first_name"))).doesMatch(
                Annotation(ObjectType("javax/persistence/Column"),
                    ArrayBuffer[ElementValuePair](ElementValuePair("name", StringValue("first_name")))))
        matched2 should be(true)
    }

    test("the AnnotationMatcher should not match the given annotation") {
        val matched1 = AnnotationMatcher("javax.persistence.Column").doesMatch(
            Annotation(ObjectType("javax/persistence/Transient"), IndexedSeq.empty))
        matched1 should be(false)

        val matched2 = AnnotationMatcher("javax.persistence.Column",
            Map("name" -> StringValue("first_name"))).doesMatch(
                Annotation(ObjectType("javax/persistence/Transient"),
                    ArrayBuffer[ElementValuePair](ElementValuePair("name", StringValue("last_name")))))
        matched2 should be(false)
    }

    /*
     * ClassMatcher matching classes only
     */
    test("the ClassMatcher should match only classes") {
        val matchedClasses1 = ClassMatcher(nameMatcher = SimpleNameMatcher("entity.impl.User"), matchMethods = false, matchFields = false).extension(project)
        matchedClasses1 should not be (empty)
        matchedClasses1.size should be(1)

        val matchedClasses2 = ClassMatcher(nameMatcher = SimpleNameMatcher("entity/impl/User", false), matchMethods = false, matchFields = false).extension(project)
        matchedClasses2 should not be (empty)
        matchedClasses2.size should be(1)

        val matchedClasses3 = ClassMatcher(nameMatcher = SimpleNameMatcher("entity", true), matchMethods = false, matchFields = false).extension(project)
        matchedClasses3 should not be (empty)
        matchedClasses3.size should be(3)

        val matchedClasses4 = ClassMatcher(nameMatcher = RegexNameMatcher(""".+User""".r), matchMethods = false, matchFields = false).extension(project)
        matchedClasses4 should not be (empty)
        matchedClasses4.size should be(1)
    }

    test("the ClassMatcher should not match any class") {
        val matchedClasses1 = ClassMatcher("entity.impl.Contact").extension(project)
        matchedClasses1 should be(empty)

        val matchedClasses2 = ClassMatcher(SimpleNameMatcher("entity/impl/Contact", false)).extension(project)
        matchedClasses2 should be(empty)

        val matchedClasses3 = ClassMatcher(SimpleNameMatcher("entity/impl/C", true)).extension(project)
        matchedClasses3 should be(empty)

        val matchedClasses4 = ClassMatcher(RegexNameMatcher(""".+Contact""".r)).extension(project)
        matchedClasses4 should be(empty)
    }

    /*
     * ClassMatcher with AccessFlags matching classes only
     */
    test("the ClassMatcher should match only classes with the given access flags") {
        val matchedClasses1 = ClassMatcher(AccessFlagsMatcher.NOT_ABSTRACT, matchMethods = false, matchFields = false).extension(project)
        matchedClasses1 should not be (empty)
        matchedClasses1.size should be(2)

        val matchedClasses2 = ClassMatcher(AccessFlagsMatcher.PUBLIC_ABSTRACT, matchMethods = false, matchFields = false).extension(project)
        matchedClasses2 should not be (empty)
        matchedClasses2.size should be(1)

        val matchedClasses3 = ClassMatcher(AccessFlagsMatcher.PUBLIC, matchMethods = false, matchFields = false).extension(project)
        matchedClasses3 should not be (empty)
        matchedClasses3.size should be(3)

        val matchedClasses4 = ClassMatcher(AccessFlagsMatcher.ALL, matchMethods = false, matchFields = false).extension(project)
        matchedClasses4 should not be (empty)
        matchedClasses4.size should be(3)

        val matchedClasses5 = ClassMatcher(AccessFlagsMatcher.PUBLIC && AccessFlagsMatcher.NOT_ABSTRACT, matchMethods = false, matchFields = false).extension(project)
        matchedClasses5 should not be (empty)
        matchedClasses5.size should be(2)
    }

    test("the ClassMatcher should not match any class with the given access flags") {
        val matchedClasses1 = ClassMatcher(AccessFlagsMatcher.PRIVATE_FINAL, matchMethods = false, matchFields = false).extension(project)
        matchedClasses1 should be(empty)

        val matchedClasses2 = ClassMatcher(AccessFlagsMatcher.PUBLIC_INTERFACE, matchMethods = false, matchFields = false).extension(project)
        matchedClasses2 should be(empty)

        val matchedClasses3 = ClassMatcher(AccessFlagsMatcher.PUBLIC_STATIC_FINAL, matchMethods = false, matchFields = false).extension(project)
        matchedClasses3 should be(empty)

        val matchedClasses4 = ClassMatcher(AccessFlagsMatcher.PUBLIC_STATIC, matchMethods = false, matchFields = false).extension(project)
        matchedClasses4 should be(empty)
    }

    /*
     * ClassMatcher with Annotation matching classes only
     */
    test("the ClassMatcher should match only classes with the given annotation") {
        val matchedClasses1 = ClassMatcher(annotationMatcher = Some(AnnotationMatcher("javax.persistence.Entity")), matchMethods = false, matchFields = false).extension(project)
        matchedClasses1 should not be (empty)
        matchedClasses1.size should be(2)
    }

    test("the ClassMatcher should not match any class with the given annotation") {
        val matchedClasses1 = ClassMatcher(annotationMatcher = Some(AnnotationMatcher("javax.persistence.Transient")), matchMethods = false, matchFields = false).extension(project)
        matchedClasses1 should be(empty)
    }

    /*
     * ClassMatcher matching complete classes
     */
    test("the ClassMatcher should match any element of the class") {
        val matchedClasses1 = ClassMatcher("entity.impl.User").extension(project)
        matchedClasses1 should not be (empty)
        matchedClasses1.size should be(19)

        val matchedClasses2 = ClassMatcher(SimpleNameMatcher("entity/impl/User", false)).extension(project)
        matchedClasses2 should not be (empty)
        matchedClasses2.size should be(19)

        val matchedClasses3 = ClassMatcher(SimpleNameMatcher("entity", true)).extension(project)
        matchedClasses3 should not be (empty)
        matchedClasses3.size should be(37)

        val matchedClasses4 = ClassMatcher(RegexNameMatcher(""".+User""".r)).extension(project)
        matchedClasses4 should not be (empty)
        matchedClasses4.size should be(19)
    }

    test("the ClassMatcher should not match any element") {
        val matchedClasses1 = ClassMatcher("entity.impl.Contact").extension(project)
        matchedClasses1 should be(empty)

        val matchedClasses2 = ClassMatcher(SimpleNameMatcher("entity/impl/Contact", false)).extension(project)
        matchedClasses2 should be(empty)

        val matchedClasses3 = ClassMatcher(SimpleNameMatcher("entity/impl/C", true)).extension(project)
        matchedClasses3 should be(empty)

        val matchedClasses4 = ClassMatcher(RegexNameMatcher(""".+Contact""".r)).extension(project)
        matchedClasses4 should be(empty)
    }

    /*
     * ClassMatcher with Annotation matching complete classes
     */
    test("the ClassMatcher should match any element of the classes annotated with the given annotation") {
        val matchedClasses1 = ClassMatcher(AnnotationMatcher("javax.persistence.Entity")).extension(project)
        matchedClasses1 should not be (empty)
        matchedClasses1.size should be(34)
    }

    test("the ClassMatcher should not match any element with the given annotation") {
        val matchedClasses1 = ClassMatcher(AnnotationMatcher("javax.persistence.Transient")).extension(project)
        matchedClasses1 should be(empty)
    }

    /*
     * PackageMatcher matching classes only
     */
    test("the PackageMatcher should match only classes") {
        val classMatcher = ClassMatcher(matchMethods = false, matchFields = false)

        val matchedClasses1 = PackageMatcher("entity", classMatcher).extension(project)
        matchedClasses1 should not be (empty)
        matchedClasses1.size should be(1)

        val matchedClasses2 = PackageMatcher("entity", classMatcher, true).extension(project)
        matchedClasses2 should not be (empty)
        matchedClasses2.size should be(3)

        val matchedClasses3 = PackageMatcher(SimpleNameMatcher("entity", false), classMatcher).extension(project)
        matchedClasses3 should not be (empty)
        matchedClasses3.size should be(1)

        val matchedClasses4 = PackageMatcher(RegexNameMatcher(""".+impl""".r), classMatcher).extension(project)
        matchedClasses4 should not be (empty)
        matchedClasses4.size should be(2)
    }

    test("the PackageMatcher should not match any class") {
        val classMatcher = ClassMatcher(matchMethods = false, matchFields = false)

        val matchedClasses1 = PackageMatcher("entity.user", classMatcher).extension(project)
        matchedClasses1 should be(empty)

        val matchedClasses2 = PackageMatcher(SimpleNameMatcher("entity/user", false), classMatcher).extension(project)
        matchedClasses2 should be(empty)

        val matchedClasses3 = PackageMatcher(SimpleNameMatcher("entity/u", true), classMatcher).extension(project)
        matchedClasses3 should be(empty)

        val matchedClasses4 = PackageMatcher(RegexNameMatcher(""".+user""".r), classMatcher).extension(project)
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
        matchedClasses2.size should be(37)

        val matchedClasses3 = PackageMatcher(SimpleNameMatcher("entity", false), None).extension(project)
        matchedClasses3 should not be (empty)
        matchedClasses3.size should be(3)

        val matchedClasses4 = PackageMatcher(RegexNameMatcher(""".+impl""".r), None).extension(project)
        matchedClasses4 should not be (empty)
        matchedClasses4.size should be(34)
    }

    test("the PackageMatcher should not match any element") {
        val matchedClasses1 = PackageMatcher("entity.user").extension(project)
        matchedClasses1 should be(empty)

        val matchedClasses2 = PackageMatcher(SimpleNameMatcher("entity/user", false), None).extension(project)
        matchedClasses2 should be(empty)

        val matchedClasses3 = PackageMatcher(SimpleNameMatcher("entity/u", true), None).extension(project)
        matchedClasses3 should be(empty)

        val matchedClasses4 = PackageMatcher(RegexNameMatcher(""".+user""".r), None).extension(project)
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
            AnnotationMatcher("javax.persistence.Column", Map("name" -> StringValue("first_name"), "nullable" -> BooleanValue(false)))).extension(project)
        matchedFields2 should not be (empty)
        matchedFields2.size should be(1)

        val matchedFields3 = FieldMatcher(Some(ClassMatcher("entity.impl.User")), fieldType = Some("Ljava.lang.String;")).extension(project)
        matchedFields3 should not be (empty)
        matchedFields3.size should be(3)

        val matchedFields4 = FieldMatcher(Some(ClassMatcher("entity.impl.User")), fieldName = Some("firstName")).extension(project)
        matchedFields4 should not be (empty)
        matchedFields4.size should be(1)
    }

    test("the FieldMatcher should not match any element") {
        val matchedFields1 = FieldMatcher(ClassMatcher("entity.impl.Contact")).extension(project)
        matchedFields1 should be(empty)

        val matchedFields2 = FieldMatcher(
            ClassMatcher("entity.impl.User"),
            AnnotationMatcher("javax.persistence.Column", Map("name" -> StringValue("street"), "nullable" -> BooleanValue(false)))).extension(project)
        matchedFields2 should be(empty)

        val matchedFields3 = FieldMatcher(Some(ClassMatcher("entity.impl.User")), fieldType = Some("Ljava.lang.Integer;")).extension(project)
        matchedFields3 should be(empty)

        val matchedFields4 = FieldMatcher(Some(ClassMatcher("entity.impl.User")), fieldName = Some("street")).extension(project)
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
            AnnotationMatcher("javax.persistence.Transient")).extension(project)
        matchedMethods2 should not be (empty)
        matchedMethods2.size should be(1)

        val matchedMethods3 = MethodMatcher(ClassMatcher("entity.impl.User"), MethodAttributesMatcher("getFullName")).extension(project)
        matchedMethods3 should not be (empty)
        matchedMethods3.size should be(1)
    }

    test("the MethodMatcher should not match any element") {
        val matchedFields1 = MethodMatcher(ClassMatcher("entity.impl.Contact")).extension(project)
        matchedFields1 should be(empty)

        val matchedFields2 = MethodMatcher(
            ClassMatcher("entity.impl.User"),
            AnnotationMatcher("javax.persistence.Column", Map("name" -> StringValue("street"), "nullable" -> BooleanValue(false)))).extension(project)
        matchedFields2 should be(empty)

        val matchedFields3 = MethodMatcher(ClassMatcher("entity.impl.User"), MethodAttributesMatcher("getStreet")).extension(project)
        matchedFields3 should be(empty)
    }
}
