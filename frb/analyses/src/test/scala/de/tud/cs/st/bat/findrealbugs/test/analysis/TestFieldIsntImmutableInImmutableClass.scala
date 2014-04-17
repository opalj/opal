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
package de.tud.cs.st
package bat
package findrealbugs
package test
package analysis

import AnalysisTest._
import analyses._
import resolved._
import resolved.analyses._
import java.net.URL

/**
 * Unit test for FieldIsntImmutableInImmutableClass
 *
 * @author Roberts Kolosovs
 * @author Peter Spieler
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TestFieldIsntImmutableInImmutableClass extends AnalysisTest {
    import TestFieldIsntImmutableInImmutableClass._

    behavior of "FieldIsntImmutableInImmutableClass"

    it should "report a mutable field whatever in CustomAnnotatedWithTrivialMutable" in {
        val declaringClass = ObjectType(
            "FieldIsntImmutableInImmutableClass/CustomAnnotatedWithTrivialMutable")
        results should contain(
            FieldBasedReport(
                project.source(declaringClass),
                Severity.Warning,
                declaringClass,
                Some(IntegerType),
                "whatever",
                "is mutable, because it is not private, and not final."))
    }

    it should "report a mutable field foo in "+
        "JCIPAnnotatedWithMutablePublicFinalArray" in {
            val declaringClass = ObjectType(
                "FieldIsntImmutableInImmutableClass/"+
                    "JCIPAnnotatedWithMutablePublicFinalArray")
            results should contain(
                FieldBasedReport(
                    project.source(declaringClass),
                    Severity.Warning,
                    declaringClass,
                    Some(ArrayType(IntegerType)),
                    "foo",
                    "is mutable, because it is a non private final reference "+
                        "to a mutable object."))
        }

    it should "report a mutable field mutable in "+
        "JCIPAnnotatedWithMutablePublicFinalField" in {
            val declaringClass = ObjectType(
                "FieldIsntImmutableInImmutableClass/"+
                    "JCIPAnnotatedWithMutablePublicFinalField")
            results should contain(
                FieldBasedReport(
                    project.source(declaringClass),
                    Severity.Warning,
                    declaringClass,
                    Some(ObjectType("FieldIsntImmutableInImmutableClass/"+
                        "NotImmutableWithPublicFields")),
                    "mutable",
                    "is mutable, because it is a non private final reference "+
                        "to a mutable object."))
        }

    it should "report mutable field whatever in class "+
        "JCIPAnnotatedWithTrivialMutable" in {
            val declaringClass = ObjectType(
                "FieldIsntImmutableInImmutableClass/JCIPAnnotatedWithTrivialMutable")
            results should contain(
                FieldBasedReport(
                    project.source(declaringClass),
                    Severity.Warning,
                    declaringClass,
                    Some(IntegerType),
                    "whatever",
                    "is mutable, because it is not private, and not final."))
        }

    it should "report mutable field x in class JCIPAnnotatedWithDirectPublicSetter" in {
        val declaringClass = ObjectType(
            "FieldIsntImmutableInImmutableClass/JCIPAnnotatedWithDirectPublicSetter")
        results should contain(
            FieldBasedReport(
                project.source(declaringClass),
                Severity.Warning,
                declaringClass,
                Some(IntegerType),
                "x",
                "is mutable because it has an (indirect) public setter"))
    }

    it should "report mutable field x in class JCIPAnnotatedWithIndirectPublicSetter" in {
        val declaringClass = ObjectType(
            "FieldIsntImmutableInImmutableClass/JCIPAnnotatedWithIndirectPublicSetter")
        results should contain(
            FieldBasedReport(
                project.source(declaringClass),
                Severity.Warning,
                declaringClass,
                Some(IntegerType),
                "x",
                "is mutable because it has an (indirect) public setter"))
    }

    it should "report field foo in JCIPAnnotatedWithoutDefensiveCopy" in {
        val declaringClass = ObjectType(
            "FieldIsntImmutableInImmutableClass/JCIPAnnotatedWithoutDefensiveCopy")
        results should contain(
            FieldBasedReport(
                project.source(declaringClass),
                Severity.Warning,
                declaringClass,
                Some(ArrayType(IntegerType)),
                "foo",
                "is mutable, because it isn't defensively copied every time it is passed"+
                    " in or out of the class, or the defensive copy is not deep enough."))
    }

    it should "report field foo in JCIPAnnotatedWithTooShallowDefensiveCopy" in {
        val declaringClass = ObjectType(
            "FieldIsntImmutableInImmutableClass/JCIPAnnotatedWithTooShallowDefensiveCopy")
        results should contain(
            FieldBasedReport(
                project.source(declaringClass),
                Severity.Warning,
                declaringClass,
                Some(ObjectType("FieldIsntImmutableInImmutableClass/"+
                    "NotImmutableWithPublicFields")),
                "foo",
                "is mutable, because it isn't defensively copied every time it is passed"+
                    " in or out of the class, or the defensive copy is not deep enough."))
    }

    it should "report field bar in JCIPAnnotatedWithTooShallowDefensiveCopy" in {
        val declaringClass = ObjectType(
            "FieldIsntImmutableInImmutableClass/JCIPAnnotatedWithTooShallowDefensiveCopy")
        results should contain(FieldBasedReport(
            project.source(declaringClass),
            Severity.Warning,
            declaringClass,
            Some(ArrayType(ObjectType("FieldIsntImmutableInImmutableClass/"+
                "NotImmutableWithPublicFields"))),
            "bar",
            "is mutable, because it isn't defensively copied every time it is passed "+
                "in or out of the class, or the defensive copy is not deep enough."))
    }

    it should "report field whatever in class JSR305AnnotatedWithTrivialMutable" in {
        val declaringClass = ObjectType(
            "FieldIsntImmutableInImmutableClass/JSR305AnnotatedWithTrivialMutable")
        results should contain(FieldBasedReport(
            project.source(declaringClass),
            Severity.Warning,
            declaringClass,
            Some(IntegerType),
            "whatever",
            "is mutable, because it is not private, and not final."))
    }

    it should "report field foo in JCIPAnnotatedWithoutDefensiveCopyAtInput" in {
        val declaringClass = ObjectType(
            "FieldIsntImmutableInImmutableClass/JCIPAnnotatedWithoutDefensiveCopyAtInput")
        results should contain(FieldBasedReport(
            project.source(declaringClass),
            Severity.Warning,
            declaringClass,
            Some(ArrayType(IntegerType)),
            "foo",
            "is mutable, because it isn't defensively copied every time it is passed "+
                "in or out of the class, or the defensive copy is not deep enough."))
    }

    it should "report field foo in JCIPAnnotatedWithCyclicFields" in {
        val declaringClass = ObjectType(
            "FieldIsntImmutableInImmutableClass/JCIPAnnotatedWithCyclicFields")
        results should contain(FieldBasedReport(
            project.source(declaringClass),
            Severity.Warning,
            declaringClass,
            Some(ObjectType(
                "FieldIsntImmutableInImmutableClass/MutableClassWithCyclicFieldsA")),
            "foo",
            "is mutable, because it isn't defensively copied every time it is passed "+
                "in or out of the class, or the defensive copy is not deep enough."))
    }

    it should "report field bar in JCIPAnnotatedWithCyclicFields" in {
        val declaringClass = ObjectType(
            "FieldIsntImmutableInImmutableClass/JCIPAnnotatedWithCyclicFields")
        results should contain(FieldBasedReport(
            project.source(declaringClass),
            Severity.Warning,
            declaringClass,
            Some(ObjectType(
                "FieldIsntImmutableInImmutableClass/MutableClassWithCyclicFieldsB")),
            "bar",
            "is mutable, because it isn't defensively copied every time it is passed "+
                "in or out of the class, or the defensive copy is not deep enough."))
    }

    it should "report field foo in JCIPAnnotatedWithDefensiveCopyOfUnknownClass" in {
        val declaringClass = ObjectType(
            "FieldIsntImmutableInImmutableClass/"+
                "JCIPAnnotatedWithDefensiveCopyOfUnknownClass")
        results should contain(FieldBasedReport(
            project.source(declaringClass),
            Severity.Warning,
            declaringClass,
            Some(ObjectType(
                "FieldIsntImmutableInImmutableClass/MutableClassWithUnknownField")),
            "foo",
            "is mutable, because it isn't defensively copied every time it is passed "+
                "in or out of the class, or the defensive copy is not deep enough."))
    }

    it should "report that MutableClassWithCyclicFieldsB is part of a "+
        "cyclic composition" in {
            val theClass = ObjectType(
                "FieldIsntImmutableInImmutableClass/MutableClassWithCyclicFieldsB")
            results should contain(ClassBasedReport(
                project.source(theClass),
                Severity.Info,
                theClass,
                "is part of a cyclic composition. We treat it as mutable."))
        }

    it should "report that MutableClassWithCyclicFieldsA is part of a "+
        "cyclic composition" in {
            val theClass = ObjectType(
                "FieldIsntImmutableInImmutableClass/MutableClassWithCyclicFieldsA")
            results should contain(ClassBasedReport(
                project.source(theClass),
                Severity.Info,
                theClass,
                "is part of a cyclic composition. We treat it as mutable."))
        }

    it should "find 18 issues in total" in {
        results.size should be(18)
    }
}

object TestFieldIsntImmutableInImmutableClass {
    val project = makeProjectFromJars(
        Seq("JCIPAnnotations.jar",
            "FieldIsntImmutableInImmutableClass.jar",
            "OwnAnnotations.jar",
            "JSR305Annotations.jar")
    )

    val results = new FieldIsntImmutableInImmutableClass[URL].analyze(project).toSet
}
