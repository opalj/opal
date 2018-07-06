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
package properties

import org.junit.runner.RunWith
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.reader.Java8Framework.ClassFiles
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CallersPropertyTest extends FlatSpec with Matchers {
    val typesProject =
        Project(
            ClassFiles(locateTestResources("classhierarchy.jar", "bi")),
            Traversable.empty,
            true
        )

    implicit val declaredMethods: DeclaredMethods = typesProject.get(DeclaredMethodsKey)

    val declaredMethod: DeclaredMethod = declaredMethods.declaredMethods.find(_ ⇒ true).get
    val otherMethod = declaredMethods.declaredMethods.find(_ ne declaredMethod).get

    behavior of "the no caller object"

    it should "update correctly" in {
        assert(NoCallers.updateVMLevelCall() eq OnlyVMLevelCallers)
        assert(NoCallers.updateWithUnknownContext() eq OnlyCallersWithUnknownContext)
        val oneCaller = NoCallers.updated(declaredMethod, pc = 0)
        assert(oneCaller.isInstanceOf[CallersOnlyWithConcreteCallers])
        assert(oneCaller.callers.size == 1 && oneCaller.callers.exists {
            case (dm, pc) ⇒ (dm eq declaredMethod) && (pc == 0)
        })

    }

    it should "behave correctly" in {
        assert(NoCallers.size == 0)
        assert(NoCallers.callers(declaredMethods = null).isEmpty)
        assert(!NoCallers.hasVMLevelCallers)
        assert(!NoCallers.hasCallersWithUnknownContext)
    }

    behavior of "only vm level callers"

    it should "update correctly" in {
        assert(OnlyVMLevelCallers.updateVMLevelCall() eq OnlyVMLevelCallers)
        assert(OnlyVMLevelCallers.updateWithUnknownContext() eq OnlyVMCallersAndWithUnknownContext)
        val oneCaller = OnlyVMLevelCallers.updated(declaredMethod, pc = 0)
        assert(oneCaller.isInstanceOf[CallersImplWithOtherCalls])
        assert(oneCaller.callers.size == 1 && oneCaller.callers.exists {
            case (dm, pc) ⇒ (dm eq declaredMethod) && (pc == 0)
        })
        assert(oneCaller.hasVMLevelCallers)
        assert(!oneCaller.hasCallersWithUnknownContext)
    }

    it should "behave correctly" in {
        assert(OnlyVMLevelCallers.size == 0)
        assert(OnlyVMLevelCallers.callers(declaredMethods = null).isEmpty)
        assert(OnlyVMLevelCallers.hasVMLevelCallers)
        assert(!OnlyVMLevelCallers.hasCallersWithUnknownContext)
    }

    behavior of "only unknown callers"

    it should "update correctly" in {
        assert(OnlyCallersWithUnknownContext.updateVMLevelCall() eq OnlyVMCallersAndWithUnknownContext)
        assert(OnlyCallersWithUnknownContext.updateWithUnknownContext() eq OnlyCallersWithUnknownContext)
        val oneCaller = OnlyCallersWithUnknownContext.updated(declaredMethod, pc = 0)
        assert(oneCaller.isInstanceOf[CallersImplWithOtherCalls])
        assert(oneCaller.callers.size == 1 && oneCaller.callers.exists {
            case (dm, pc) ⇒ (dm eq declaredMethod) && (pc == 0)
        })
        assert(!oneCaller.hasVMLevelCallers)
        assert(oneCaller.hasCallersWithUnknownContext)
    }

    it should "behave correctly" in {
        assert(OnlyCallersWithUnknownContext.size == 0)
        assert(OnlyCallersWithUnknownContext.callers(declaredMethods = null).isEmpty)
        assert(!OnlyCallersWithUnknownContext.hasVMLevelCallers)
        assert(OnlyCallersWithUnknownContext.hasCallersWithUnknownContext)
    }

    behavior of "only unknown and vm level callers"

    it should "update correctly" in {
        assert(
            OnlyVMCallersAndWithUnknownContext.updateVMLevelCall() eq OnlyVMCallersAndWithUnknownContext
        )
        assert(
            OnlyVMCallersAndWithUnknownContext.updateWithUnknownContext() eq OnlyVMCallersAndWithUnknownContext
        )
        val oneCaller = OnlyVMCallersAndWithUnknownContext.updated(declaredMethod, pc = 0)
        assert(oneCaller.isInstanceOf[CallersImplWithOtherCalls])
        assert(oneCaller.callers.size == 1 && oneCaller.callers.exists {
            case (dm, pc) ⇒ (dm eq declaredMethod) && (pc == 0)
        })
        assert(oneCaller.hasVMLevelCallers)
        assert(oneCaller.hasCallersWithUnknownContext)
    }

    it should "behave correctly" in {
        assert(OnlyVMCallersAndWithUnknownContext.size == 0)
        assert(OnlyVMCallersAndWithUnknownContext.callers(declaredMethods = null).isEmpty)
        assert(OnlyVMCallersAndWithUnknownContext.hasVMLevelCallers)
        assert(OnlyVMCallersAndWithUnknownContext.hasCallersWithUnknownContext)
    }

    behavior of "only with concrete callers"

    it should "update correctly" in {
        val callers = NoCallers.updated(declaredMethod, 0)

        val withTwoCallers = callers.updated(otherMethod, 1)
        assert(withTwoCallers.isInstanceOf[CallersOnlyWithConcreteCallers])
        assert(withTwoCallers.size == 2)

        val updateWithSame = callers.updated(declaredMethod, 0)
        assert(callers eq updateWithSame)

        val withVMLevelCallers = callers.updateVMLevelCall()
        assert(withVMLevelCallers.hasVMLevelCallers)
        assert(withVMLevelCallers.isInstanceOf[CallersImplWithOtherCalls])
        assert(!withVMLevelCallers.hasCallersWithUnknownContext)

        val withUnknownContext = callers.updateWithUnknownContext()
        assert(!withUnknownContext.hasVMLevelCallers)
        assert(withUnknownContext.isInstanceOf[CallersImplWithOtherCalls])
        assert(withUnknownContext.hasCallersWithUnknownContext)
    }

    it should "behave correctly" in {
        val callers = NoCallers.updated(declaredMethod, 0)
        assert(callers.size == 1)
        assert(callers.callers.exists { case (dm, pc) ⇒ (dm eq declaredMethod) && (pc == 0) })
        assert(!callers.hasCallersWithUnknownContext)
        assert(!callers.hasVMLevelCallers)
    }

    behavior of "any kind of callers"

    it should "update correctly" in {
        val callersWithVMLevelCall = OnlyVMLevelCallers.updated(declaredMethod, 0)
        assert(callersWithVMLevelCall.updateVMLevelCall() eq callersWithVMLevelCall)
        val callersWithBothUnknownCalls1 = callersWithVMLevelCall.updateWithUnknownContext()
        assert(callersWithBothUnknownCalls1.hasCallersWithUnknownContext)
        assert(callersWithBothUnknownCalls1.hasVMLevelCallers)
        assert(callersWithBothUnknownCalls1.callers.size == 1)
        assert(callersWithBothUnknownCalls1.callers.exists {
            case (dm, pc) ⇒ (dm eq declaredMethod) && (pc == 0)
        })

        val callersWithUnknownCallers = OnlyCallersWithUnknownContext.updated(declaredMethod, pc = 0)
        assert(callersWithUnknownCallers.updateWithUnknownContext() eq callersWithUnknownCallers)
        val callersWithBothUnknownCalls2 = callersWithUnknownCallers.updateVMLevelCall()
        assert(callersWithBothUnknownCalls2.hasCallersWithUnknownContext)
        assert(callersWithBothUnknownCalls2.hasVMLevelCallers)
        assert(callersWithBothUnknownCalls2.callers.size == 1)
        assert(callersWithBothUnknownCalls2.callers.exists {
            case (dm, pc) ⇒ (dm eq declaredMethod) && (pc == 0)
        })

        val twoCallers = callersWithVMLevelCall.updated(otherMethod, pc = 1)
        assert(twoCallers.size == 2)
        assert(twoCallers.callers.exists {
            case (dm, pc) ⇒ (dm eq declaredMethod) && (pc == 0)
        })
        assert(twoCallers.callers.exists {
            case (dm, pc) ⇒ (dm eq otherMethod) && (pc == 1)
        })
    }

    it should "behave correctly" in {
        val encodedCallers = Set(CallersProperty.toLong(declaredMethods.methodID(declaredMethod), 0))
        val withVM = CallersImplWithOtherCalls(encodedCallers, true, false)
        assert(withVM.size == 1)
        assert(withVM.callers.exists { case (dm, pc) ⇒ (dm eq declaredMethod) && (pc == 0) })
        assert(!withVM.hasCallersWithUnknownContext)
        assert(withVM.hasVMLevelCallers)

        val withUnknwonContext = CallersImplWithOtherCalls(encodedCallers, false, true)
        assert(withUnknwonContext.size == 1)
        assert(withUnknwonContext.callers.exists { case (dm, pc) ⇒ (dm eq declaredMethod) && (pc == 0) })
        assert(withUnknwonContext.hasCallersWithUnknownContext)
        assert(!withUnknwonContext.hasVMLevelCallers)

        val withBoth = CallersImplWithOtherCalls(encodedCallers, true, true)
        assert(withBoth.size == 1)
        assert(withBoth.callers.exists { case (dm, pc) ⇒ (dm eq declaredMethod) && (pc == 0) })
        assert(withBoth.hasCallersWithUnknownContext)
        assert(withBoth.hasVMLevelCallers)
    }

}
