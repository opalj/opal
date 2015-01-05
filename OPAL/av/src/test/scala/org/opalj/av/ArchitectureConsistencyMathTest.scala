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

import org.junit.runner.RunWith

import org.opalj.av.Specification._

import org.scalatest._
import org.scalatest.junit.JUnitRunner

/**
 * Systematic tests created to check the behavior of the Specification package.
 * Tests that the implemented architecture of the Mathematics test classes
 * are consistent with its specifiation/with the intended architecture.
 *
 * @author Samuel Beracasa
 * @author Marco Torsello
 */
@RunWith(classOf[JUnitRunner])
class ArchitectureConsistencyMathTest extends FlatSpec with Matchers with BeforeAndAfterAll {

    val project = (SourceDirectory("OPAL/av/target/scala-2.11/test-classes"))

    behavior of "the Architecture Validation Library on the Mathematics test classes"

    /*
     * outgoing is_only_allowed_to_use constraint validations
     */
    it should "validate the is_only_allowed_to_use constraint with no violations" in {
        val specification =
            new Specification(project) {
                ensemble('Operations) {
                    "org.opalj.av.testclasses.Operations*"
                }

                ensemble('Number) {
                    "org.opalj.av.testclasses.Number*"
                }

                ensemble('Rational) {
                    "org.opalj.av.testclasses.Rational*"
                }

                ensemble('Mathematics) {
                    "org.opalj.av.testclasses.Mathematics*"
                }

                ensemble('Example) {
                    "org.opalj.av.testclasses.Example*"
                }

                'Example is_only_allowed_to_use 'Mathematics
            }

        val result = specification.analyze().map(_.toString).toSeq.sorted
        val output = result.mkString("\n")
        output should be(empty)
    }

    it should ("validate the is_only_allowed_to_use constraint with violations") in {
        val specification =
            new Specification(project) {
                ensemble('Operations) {
                    "org.opalj.av.testclasses.Operations*"
                }

                ensemble('Number) {
                    "org.opalj.av.testclasses.Number*"
                }

                ensemble('Rational) {
                    "org.opalj.av.testclasses.Rational*"
                }

                ensemble('Mathematics) {
                    "org.opalj.av.testclasses.Mathematics*"
                }

                ensemble('Example) {
                    "org.opalj.av.testclasses.Example*"
                }

                'Mathematics is_only_allowed_to_use 'Rational
            }

        val result = specification.analyze().map(_.toString).toSeq.sorted
        val output = result.mkString("\n")
        output should not be (empty)
    }

    /*
     * outgoing is_not_allowed_to_use constraint validation
     */
    it should ("validate the is_not_allowed_to_use constraint with no violations") in {
        val specification =
            new Specification(project) {
                ensemble('Operations) {
                    "org.opalj.av.testclasses.Operations*"
                }

                ensemble('Number) {
                    "org.opalj.av.testclasses.Number*"
                }

                ensemble('Rational) {
                    "org.opalj.av.testclasses.Rational*"
                }

                ensemble('Mathematics) {
                    "org.opalj.av.testclasses.Mathematics*"
                }

                ensemble('Example) {
                    "org.opalj.av.testclasses.Example*"
                }

                'Example is_not_allowed_to_use 'Rational
            }

        val result = specification.analyze().map(_.toString).toSeq.sorted
        val output = result.mkString("\n")
        output should be(empty)
    }

    it should ("validate the is_not_allowed_to_use constraint with violations") in {
        val specification =
            new Specification(project) {
                ensemble('Operations) {
                    "org.opalj.av.testclasses.Operations*"
                }

                ensemble('Number) {
                    "org.opalj.av.testclasses.Number*"
                }

                ensemble('Rational) {
                    "org.opalj.av.testclasses.Rational*"
                }

                ensemble('Mathematics) {
                    "org.opalj.av.testclasses.Mathematics*"
                }

                ensemble('Example) {
                    "org.opalj.av.testclasses.Example*"
                }

                'Mathematics is_not_allowed_to_use 'Number
            }

        val result = specification.analyze().map(_.toString).toSeq.sorted
        val output = result.mkString("\n")
        output should not be (empty)
    }

    /*
     * incoming is_only_to_be_used_by constraint validation
     */
    it should ("validate the is_only_to_be_used_by constraint with no violations") in {
        val specification =
            new Specification(project) {
                ensemble('Operations) {
                    "org.opalj.av.testclasses.Operations*"
                }

                ensemble('Number) {
                    "org.opalj.av.testclasses.Number*"
                }

                ensemble('Rational) {
                    "org.opalj.av.testclasses.Rational*"
                }

                ensemble('Mathematics) {
                    "org.opalj.av.testclasses.Mathematics*"
                }

                ensemble('Example) {
                    "org.opalj.av.testclasses.Example*"
                }

                'Mathematics is_only_to_be_used_by 'Example
            }

        val result = specification.analyze().map(_.toString).toSeq.sorted
        val output = result.mkString("\n")
        output should be(empty)
    }

    it should ("validate the is_only_to_be_used_by constraint with violations") in {
        val specification =
            new Specification(project) {
                ensemble('Operations) {
                    "org.opalj.av.testclasses.Operations*"
                }

                ensemble('Number) {
                    "org.opalj.av.testclasses.Number*"
                }

                ensemble('Rational) {
                    "org.opalj.av.testclasses.Rational*"
                }

                ensemble('Mathematics) {
                    "org.opalj.av.testclasses.Mathematics*"
                }

                ensemble('Example) {
                    "org.opalj.av.testclasses.Example*"
                }

                'Rational is_only_to_be_used_by 'Mathematics
            }

        val result = specification.analyze().map(_.toString).toSeq.sorted
        val output = result.mkString("\n")
        output should not be (empty)
    }

    /*
     * incoming allows_incoming_dependencies_from constraint validation
     */
    it should ("validate the allows_incoming_dependencies_from constraint with no violations") in {
        val specification =
            new Specification(project) {
                ensemble('Operations) {
                    "org.opalj.av.testclasses.Operations*"
                }

                ensemble('Number) {
                    "org.opalj.av.testclasses.Number*"
                }

                ensemble('Rational) {
                    "org.opalj.av.testclasses.Rational*"
                }

                ensemble('Mathematics) {
                    "org.opalj.av.testclasses.Mathematics*"
                }

                ensemble('Example) {
                    "org.opalj.av.testclasses.Example*"
                }

                'Mathematics allows_incoming_dependencies_from 'Example
            }

        val result = specification.analyze().map(_.toString).toSeq.sorted
        val output = result.mkString("\n")
        output should be(empty)
    }

    it should ("validate the allows_incoming_dependencies_from constraint with violations") in {
        val specification =
            new Specification(project) {
                ensemble('Operations) {
                    "org.opalj.av.testclasses.Operations*"
                }

                ensemble('Number) {
                    "org.opalj.av.testclasses.Number*"
                }

                ensemble('Rational) {
                    "org.opalj.av.testclasses.Rational*"
                }

                ensemble('Mathematics) {
                    "org.opalj.av.testclasses.Mathematics*"
                }

                ensemble('Example) {
                    "org.opalj.av.testclasses.Example*"
                }

                'Number allows_incoming_dependencies_from 'Rational
            }

        val result = specification.analyze().map(_.toString).toSeq.sorted
        val output = result.mkString("\n")
        output should not be (empty)
    }
}