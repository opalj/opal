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
package org.opalj
package br
package analyses

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers

import org.opalj.collection.immutable.Chain

import org.opalj.br.TestSupport.biProject

/**
 * Tests the `ProjectIndex`.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PropertyStoreKeyTest extends FunSpec with Matchers {

    describe("using the default PropertyStoreKey") {
        val p: SomeProject = biProject("ai.jar")
        assert(p.allMethods.nonEmpty)
        assert(p.allFields.nonEmpty)

        val ps = p.get(PropertyStoreKey)

        it("should always contain the project's methods") {
            assert(p.allMethods.forall(ps.isKnown))
        }

        it("should always contain the project's fields") {
            assert(p.allFields.forall(ps.isKnown))
        }

        it("should always contain the project's class files") {
            assert(p.allClassFiles.forall(ps.isKnown))
        }

        it("the context should always contain the project's methods") {
            assert(p.allMethods == ps.context[Iterable[Method]])
        }

        it("the context should always contain the project's fields") {
            assert(p.allFields == ps.context[Iterable[Field]])
        }

        it("the context  should always contain the project's class files") {
            assert(p.allClassFiles == ps.context[Iterable[ClassFile]])
        }
    }

    describe("using EntityDerivationFunctions PropertyStoreKey") {

        val p: SomeProject = biProject("ai.jar")

        PropertyStoreKey.addEntityDerivationFunction[Map[Method, Map[PC, AllocationSite]]](
            (p: SomeProject) ⇒ {

                var allAs: List[Chain[AllocationSite]] = Nil

                // Associate every new instruction in a method with an allocation site object
                val as = p.allMethods.flatMap { m ⇒
                    m.body match {
                        case None ⇒ Nil
                        case Some(code) ⇒
                            val as = code.collectWithIndex {
                                case (pc, instructions.NEW(_)) ⇒ new AllocationSite(m, pc)
                            }
                            if (as.nonEmpty) allAs ::= as
                            as
                    }
                }

                // In the context we store a map which makes the set of allocation sites
                // easily accessible
                val mToPCToAs = allAs.map { asPerMethod ⇒
                    val pcToAs = asPerMethod.map(as ⇒ as.pc → as).toMap
                    val m = pcToAs.head._2.method
                    m → pcToAs
                }.toMap

                (as, mToPCToAs)
            }
        )

        // WE HAVE TO USE THE KEY TO TRIGGER THE COMPUTATION OF THE ENTITY DERIVATION FUNCTION(S)
        val ps = p.get(PropertyStoreKey)

        it("should contain the additionally derived entities") {

            val allAs: Iterable[AllocationSite] =
                ps.context[Map[Method, Map[PC, AllocationSite]]].values.flatMap(_.values)

            val allocationSiteCount = allAs.size

            assert(allocationSiteCount > 0)
            info(s"contains $allocationSiteCount allocation sites")

            val allAdded: Boolean = allAs.forall(ps.isKnown)
            assert(allAdded)
        }

        it("should be possible to query the allocation sites") {
            val mToPCToAs = ps.context[Map[Method, Map[PC, AllocationSite]]]

            p.allMethodsWithBody.foreach { m ⇒
                val code = m.body.get
                if (mToPCToAs.contains(m)) {
                    code.forall(
                        (pc, i) ⇒
                            i.opcode match {
                                case instructions.NEW.opcode ⇒ mToPCToAs(m).get(pc).isDefined
                                case _                       ⇒ mToPCToAs(m).get(pc).isEmpty
                            }
                    )
                } else {
                    // check that the method does not contain a new instruction
                    code.iterate((pc, i) ⇒
                        if (i.opcode == instructions.NEW.opcode) {
                            info("allocation sites: "+mToPCToAs.mkString("\n"))
                            val error = m.toJava(p.classFile(m), s"missing allocation site $pc:$i")
                            fail(error)
                        })
                }
            }

        }
    }
}
