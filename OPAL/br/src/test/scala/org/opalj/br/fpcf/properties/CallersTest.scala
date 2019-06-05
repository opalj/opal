/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

import org.opalj.collection.immutable.LongTrieSet
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.CallersImplWithOtherCalls
import org.opalj.br.fpcf.properties.cg.CallersOnlyWithConcreteCallers
import org.opalj.br.fpcf.properties.cg.NoCallers
import org.opalj.br.fpcf.properties.cg.OnlyCallersWithUnknownContext
import org.opalj.br.fpcf.properties.cg.OnlyVMCallersAndWithUnknownContext
import org.opalj.br.fpcf.properties.cg.OnlyVMLevelCallers
import org.opalj.br.reader.Java8Framework.ClassFiles

@RunWith(classOf[JUnitRunner])
class CallersTest extends FlatSpec with Matchers {
    val typesProject =
        Project(
            ClassFiles(locateTestResources("classhierarchy.jar", "bi")),
            Traversable.empty,
            libraryClassFilesAreInterfacesOnly = true
        )

    implicit val declaredMethods: DeclaredMethods = typesProject.get(DeclaredMethodsKey)

    val declaredMethod: DeclaredMethod = declaredMethods.declaredMethods.find(_ ⇒ true).get
    val otherMethod: DeclaredMethod = declaredMethods.declaredMethods.find(_ ne declaredMethod).get

    behavior of "the no caller object"

    it should "update correctly" in {
        assert(NoCallers.updatedWithVMLevelCall() eq OnlyVMLevelCallers)
        assert(NoCallers.updatedWithUnknownContext() eq OnlyCallersWithUnknownContext)
        val oneCaller = NoCallers.updated(declaredMethod, pc = 0, isDirect = true)
        assert(oneCaller.isInstanceOf[CallersOnlyWithConcreteCallers])
        assert(oneCaller.callers.size == 1 && oneCaller.callers.exists {
            case (dm, pc, isDirect) ⇒ (dm eq declaredMethod) && (pc == 0) && isDirect
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
        assert(OnlyVMLevelCallers.updatedWithVMLevelCall() eq OnlyVMLevelCallers)
        assert(OnlyVMLevelCallers.updatedWithUnknownContext() eq OnlyVMCallersAndWithUnknownContext)
        val oneCaller = OnlyVMLevelCallers.updated(declaredMethod, pc = 0, isDirect = true)
        assert(oneCaller.isInstanceOf[CallersImplWithOtherCalls])
        assert(oneCaller.callers.size == 1 && oneCaller.callers.exists {
            case (dm, pc, isDirect) ⇒ (dm eq declaredMethod) && (pc == 0) && isDirect
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
        assert(OnlyCallersWithUnknownContext.updatedWithVMLevelCall() eq OnlyVMCallersAndWithUnknownContext)
        assert(OnlyCallersWithUnknownContext.updatedWithUnknownContext() eq OnlyCallersWithUnknownContext)
        val oneCaller = OnlyCallersWithUnknownContext.updated(declaredMethod, pc = 0, isDirect = true)
        assert(oneCaller.isInstanceOf[CallersImplWithOtherCalls])
        assert(oneCaller.callers.size == 1 && oneCaller.callers.exists {
            case (dm, pc, isDirect) ⇒ (dm eq declaredMethod) && (pc == 0) && isDirect
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
            OnlyVMCallersAndWithUnknownContext.updatedWithVMLevelCall() eq OnlyVMCallersAndWithUnknownContext
        )
        assert(
            OnlyVMCallersAndWithUnknownContext.updatedWithUnknownContext() eq OnlyVMCallersAndWithUnknownContext
        )
        val oneCaller = OnlyVMCallersAndWithUnknownContext.updated(declaredMethod, pc = 0, isDirect = true)
        assert(oneCaller.isInstanceOf[CallersImplWithOtherCalls])
        assert(oneCaller.callers.size == 1 && oneCaller.callers.exists {
            case (dm, pc, isDirect) ⇒ (dm eq declaredMethod) && (pc == 0) && isDirect
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
        val callers = NoCallers.updated(declaredMethod, pc = 0, isDirect = true)

        val withTwoCallers = callers.updated(otherMethod, pc = 1, isDirect = true)
        assert(withTwoCallers.isInstanceOf[CallersOnlyWithConcreteCallers])
        assert(withTwoCallers.size == 2)

        val updateWithSame = callers.updated(declaredMethod, pc = 0, isDirect = true)
        assert(callers eq updateWithSame)

        val withVMLevelCallers = callers.updatedWithVMLevelCall()
        assert(withVMLevelCallers.hasVMLevelCallers)
        assert(withVMLevelCallers.isInstanceOf[CallersImplWithOtherCalls])
        assert(!withVMLevelCallers.hasCallersWithUnknownContext)

        val withUnknownContext = callers.updatedWithUnknownContext()
        assert(!withUnknownContext.hasVMLevelCallers)
        assert(withUnknownContext.isInstanceOf[CallersImplWithOtherCalls])
        assert(withUnknownContext.hasCallersWithUnknownContext)
    }

    it should "behave correctly" in {
        val callers = NoCallers.updated(declaredMethod, pc = 0, isDirect = true)
        assert(callers.size == 1)
        assert(callers.callers.exists { case (dm, pc, isDirect) ⇒ (dm eq declaredMethod) && (pc == 0) && isDirect })
        assert(!callers.hasCallersWithUnknownContext)
        assert(!callers.hasVMLevelCallers)
    }

    behavior of "any kind of callers"

    it should "update correctly" in {
        val callersWithVMLevelCall = OnlyVMLevelCallers.updated(declaredMethod, pc = 0, isDirect = true)
        assert(callersWithVMLevelCall.updatedWithVMLevelCall() eq callersWithVMLevelCall)
        val callersWithBothUnknownCalls1 = callersWithVMLevelCall.updatedWithUnknownContext()
        assert(callersWithBothUnknownCalls1.hasCallersWithUnknownContext)
        assert(callersWithBothUnknownCalls1.hasVMLevelCallers)
        assert(callersWithBothUnknownCalls1.callers.size == 1)
        assert(callersWithBothUnknownCalls1.callers.exists {
            case (dm, pc, isDirect) ⇒ (dm eq declaredMethod) && (pc == 0) && isDirect
        })

        val callersWithUnknownCallers = OnlyCallersWithUnknownContext.updated(declaredMethod, pc = 0, isDirect = true)
        assert(callersWithUnknownCallers.updatedWithUnknownContext() eq callersWithUnknownCallers)
        val callersWithBothUnknownCalls2 = callersWithUnknownCallers.updatedWithVMLevelCall()
        assert(callersWithBothUnknownCalls2.hasCallersWithUnknownContext)
        assert(callersWithBothUnknownCalls2.hasVMLevelCallers)
        assert(callersWithBothUnknownCalls2.callers.size == 1)
        assert(callersWithBothUnknownCalls2.callers.exists {
            case (dm, pc, isDirect) ⇒ (dm eq declaredMethod) && (pc == 0) && isDirect
        })

        val twoCallers = callersWithVMLevelCall.updated(otherMethod, pc = 1, isDirect = true)
        assert(twoCallers.size == 2)
        assert(twoCallers.callers.exists {
            case (dm, pc, isDirect) ⇒ (dm eq declaredMethod) && (pc == 0) && isDirect
        })
        assert(twoCallers.callers.exists {
            case (dm, pc, isDirect) ⇒ (dm eq otherMethod) && (pc == 1) && isDirect
        })
    }

    it should "behave correctly" in {
        val encodedCallers = LongTrieSet(Callers.toLong(declaredMethod.id, pc = 0, isDirect = true))
        val withVM = CallersImplWithOtherCalls(
            encodedCallers, hasVMLevelCallers = true, hasCallersWithUnknownContext = false
        )
        assert(withVM.size == 1)
        assert(withVM.callers.exists { case (dm, pc, isDirect) ⇒ (dm eq declaredMethod) && (pc == 0) && isDirect })
        assert(!withVM.hasCallersWithUnknownContext)
        assert(withVM.hasVMLevelCallers)

        val withUnknwonContext = CallersImplWithOtherCalls(
            encodedCallers, hasVMLevelCallers = false, hasCallersWithUnknownContext = true
        )
        assert(withUnknwonContext.size == 1)
        assert(withUnknwonContext.callers.exists { case (dm, pc, isDirect) ⇒ (dm eq declaredMethod) && (pc == 0) && isDirect })
        assert(withUnknwonContext.hasCallersWithUnknownContext)
        assert(!withUnknwonContext.hasVMLevelCallers)

        val withBoth = CallersImplWithOtherCalls(
            encodedCallers, hasVMLevelCallers = true, hasCallersWithUnknownContext = true
        )
        assert(withBoth.size == 1)
        assert(withBoth.callers.exists { case (dm, pc, isDirect) ⇒ (dm eq declaredMethod) && (pc == 0) && isDirect })
        assert(withBoth.hasCallersWithUnknownContext)
        assert(withBoth.hasVMLevelCallers)
    }
}
