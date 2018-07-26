/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
import org.opalj.collection.immutable.LongTrieSet
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CallersPropertyTest extends FlatSpec with Matchers {
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
        val encodedCallers = LongTrieSet(CallersProperty.toLong(declaredMethod.id, 0))
        val withVM = CallersImplWithOtherCalls(
            encodedCallers, hasVMLevelCallers = true, hasCallersWithUnknownContext = false
        )
        assert(withVM.size == 1)
        assert(withVM.callers.exists { case (dm, pc) ⇒ (dm eq declaredMethod) && (pc == 0) })
        assert(!withVM.hasCallersWithUnknownContext)
        assert(withVM.hasVMLevelCallers)

        val withUnknwonContext = CallersImplWithOtherCalls(
            encodedCallers, hasVMLevelCallers = false, hasCallersWithUnknownContext = true
        )
        assert(withUnknwonContext.size == 1)
        assert(withUnknwonContext.callers.exists { case (dm, pc) ⇒ (dm eq declaredMethod) && (pc == 0) })
        assert(withUnknwonContext.hasCallersWithUnknownContext)
        assert(!withUnknwonContext.hasVMLevelCallers)

        val withBoth = CallersImplWithOtherCalls(
            encodedCallers, hasVMLevelCallers = true, hasCallersWithUnknownContext = true
        )
        assert(withBoth.size == 1)
        assert(withBoth.callers.exists { case (dm, pc) ⇒ (dm eq declaredMethod) && (pc == 0) })
        assert(withBoth.hasCallersWithUnknownContext)
        assert(withBoth.hasVMLevelCallers)
    }

    behavior of "the lower bound callers"

    it should "be a lower bound" in {
        val lb = new LowerBoundCallers(typesProject, declaredMethod)
        assert(lb.size == Int.MaxValue)
        assert(lb.hasCallersWithUnknownContext)
        assert(lb.hasVMLevelCallers)
        assert(lb.updateVMLevelCall() eq lb)
        assert(lb.updateWithUnknownContext() eq lb)
        assert(lb.updated(declaredMethod, 0) eq lb)
    }

}
