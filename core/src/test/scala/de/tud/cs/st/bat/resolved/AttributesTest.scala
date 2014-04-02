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
package de.tud.cs.st.bat
package resolved

import org.scalatest.FunSuite
import org.scalatest.ParallelTestExecution

/**
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class AttributesTest extends FunSuite with ParallelTestExecution {

    val attributesJARFile = TestSupport.locateTestResources("classfiles/Attributes.jar")

    import reader.Java8Framework.ClassFile

    test("test that the deprecated attribute is present") {
        val cf1 = ClassFile(attributesJARFile, "attributes/DeprecatedByAnnotation.class")
        assert(cf1.isDeprecated)
        assert(
            cf1.runtimeVisibleAnnotations.find({
                case Annotation(ObjectType("java/lang/Deprecated"), _) ⇒ true
                case _ ⇒ false
            }).isDefined
        )

        val cf2 = ClassFile(attributesJARFile, "attributes/DeprecatedByJavaDoc.class")
        assert(cf2.isDeprecated)
    }

    test("test that the source file attribute is present") {
        val cf1 = ClassFile(attributesJARFile, "attributes/DeprecatedByAnnotation.class")
        assert(cf1.sourceFile != None)
    }

}
