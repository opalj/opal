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
package fpcf

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers

import org.opalj.collection.immutable.Chain

import org.opalj.br.TestSupport.biProject

import org.opalj.br.PC
import org.opalj.br.PCAndInstruction
import org.opalj.br.Method
import org.opalj.br.Field
import org.opalj.br.ClassFile
import org.opalj.br.AllocationSite
import org.opalj.br.ObjectAllocationSite
import org.opalj.br.FormalParameter
import org.opalj.br.instructions.NEW
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.AllocationSites
import org.opalj.br.analyses.AllocationSitesKey
import org.opalj.br.analyses.FormalParameters
import org.opalj.br.analyses.FormalParametersKey

/**
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PropertyStoreKeyTest extends FunSpec with Matchers {

    describe("using the default PropertyStoreKey") {
        val p: SomeProject = biProject("ai.jar")
        assert(p.allMethods.nonEmpty)
        assert(p.allFields.nonEmpty)

        val ps = p.get(PropertyStoreKey)

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

    describe("using EntityDerivationFunctions") {

        val p: SomeProject = biProject("ai.jar")

        PropertyStoreKey.addEntityDerivationFunction[Map[Method, Map[PC, AllocationSite]]](p) {
            // Callback function which is called, when the information is actually required

            var allAs: List[Chain[AllocationSite]] = Nil

            // Associate every new instruction in a method with an allocation site object
            val as = p.allMethodsWithBody flatMap { m ⇒
                val code = m.body.get
                val as = code collectWithIndex {
                    case PCAndInstruction(pc, NEW(_)) ⇒ new ObjectAllocationSite(m, pc)
                }
                if (as.nonEmpty) allAs ::= as
                as
            }

            // In the context we store a map which makes the set of allocation sites
            // easily accessible.
            val mToPCToAs = allAs.map { asPerMethod ⇒
                val pcToAs = asPerMethod.map(as ⇒ as.pc → as).toMap
                val m = pcToAs.head._2.method
                m → pcToAs
            }.toMap

            (as, mToPCToAs)
        }

        // WE HAVE TO USE THE KEY TO TRIGGER THE COMPUTATION OF THE ENTITY DERIVATION FUNCTION(S)
        val ps = p.get(PropertyStoreKey)

        it("should contain the additionally derived entities") {

            val allAs: Iterable[AllocationSite] = {
                ps.context[Map[Method, Map[PC, AllocationSite]]].values.flatMap(_.values)
            }

            val allocationSiteCount = allAs.size

            assert(allocationSiteCount > 0)
            info(s"contains $allocationSiteCount allocation sites")
        }

        it("should be possible to query the allocation sites") {
            val mToPCToAs = ps.context[Map[Method, Map[PC, AllocationSite]]]

            p.allMethodsWithBody.foreach { m ⇒
                val code = m.body.get
                if (mToPCToAs.contains(m)) {
                    code.forall(
                        (pc, i) ⇒
                            i.opcode match {
                                case NEW.opcode ⇒ mToPCToAs(m).get(pc).isDefined
                                case _          ⇒ mToPCToAs(m).get(pc).isEmpty
                            }
                    )
                } else {
                    // check that the method does not contain a new instruction
                    code.iterate { (pc, i) ⇒
                        if (i.opcode == NEW.opcode) {
                            info("allocation sites: "+mToPCToAs.mkString("\n"))
                            val error = m.toJava(s"missing allocation site $pc:$i")
                            fail(error)
                        }
                    }
                }
            }

        }
    }

    describe("using makeFormalParametersAvailable") {

        val p: SomeProject = biProject("ai.jar")
        assert(p != null)
        assert(p.allClassFiles.nonEmpty)

        PropertyStoreKey.makeFormalParametersAvailable(p)

        // WE HAVE TO USE THE KEY TO TRIGGER THE COMPUTATION OF THE ENTITY DERIVATION FUNCTION(S)
        val ps = p.get(PropertyStoreKey)

        assert(p.has(FormalParametersKey).isDefined)

        it("should contain the formal parameters") {

            val allFPs: Iterable[FormalParameter] = ps.context[FormalParameters].formalParameters

            val formalParametersCount = allFPs.size

            assert(formalParametersCount >= p.allMethods.map(m ⇒ m.descriptor.parametersCount).sum)
            info(s"contains $formalParametersCount formal parameters")
        }
    }

    describe("using makeAllocationSitesAvailable") {

        val p: SomeProject = biProject("ai.jar")

        PropertyStoreKey.makeAllocationSitesAvailable(p)

        // WE HAVE TO USE THE KEY TO TRIGGER THE COMPUTATION OF THE ENTITY DERIVATION FUNCTION(S)
        val ps = p.get(PropertyStoreKey)

        assert(p.has(AllocationSitesKey).isDefined)

        it("should contain the allocation sites") {
            val allocationSites = ps.context[AllocationSites]

            assert(allocationSites.nonEmpty)
            info(s"contains ${allocationSites.size} allocation sites")
        }
    }

}
