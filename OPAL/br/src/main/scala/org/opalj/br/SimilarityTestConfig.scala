/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

package org.opalj.br

import org.opalj.br.instructions.Instruction

/**
 *
 * Specify which tests to turn on and off when comparing for similarity.
 * Returning true turns the corresponding test, false turns it off.
 * When filtering methods, instructions or attributes the size
 * comparison needs to be turned of as well.
 *
 * @author Timothy Earley
 */

trait SimilarityTestConfig {
    def testAccessFlags(accessFlags: Int): Boolean

    def testType(thisType: ObjectType): Boolean
    def testInterfaceTypes(interfaceTypes: Seq[ObjectType]): Boolean
    def testSuperclassType(superClassType: Option[ObjectType]): Boolean

    def testFieldsSize(fields: Fields): Boolean
    def testField(field: Field): Boolean

    def testInstructionsLength(instructions: Instructions): Boolean
    def testInstruction(instruction: Instruction): Boolean

    def testMethodsSize(methods: Methods): Boolean
    def testMethod(method: Method): Boolean

    def testAttributesSize(attributes: Attributes): Boolean
    def testAttribute(attribute: Attribute): Boolean

    def checkBody(body: Option[Code]): Boolean

    def testExceptionHandlers(exceptionHandlers: ExceptionHandlers): Boolean

}

class DefaultSimilarityTestConfig extends SimilarityTestConfig {
    def testAccessFlags(accessFlags: Int) = true

    def testType(thisType: ObjectType) = true
    def testInterfaceTypes(interfaceTypes: Seq[ObjectType]) = true
    def testSuperclassType(superClassType: Option[ObjectType]) = true

    def testFieldsSize(fields: Fields) = true
    def testField(field: Field) = true

    def testInstructionsLength(instructions: Instructions) = true
    def testInstruction(instruction: Instruction) = true

    def testMethodsSize(methods: Methods) = true
    def testMethod(method: Method) = true

    def testAttributesSize(attributes: Attributes) = true
    def testAttribute(attribute: Attribute) = true

    def checkBody(body: Option[Code]) = true

    def testExceptionHandlers(exceptionHandlers: ExceptionHandlers) = true
}

object DefaultSimilarityTestConfig extends DefaultSimilarityTestConfig
