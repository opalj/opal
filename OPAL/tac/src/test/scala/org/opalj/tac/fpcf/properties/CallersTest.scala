/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package properties

import scala.collection.immutable.IntMap

import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import org.opalj.collection.immutable.LongLinkedTrieSet
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.Project
import org.opalj.br.reader.Java8Framework.ClassFiles
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.SimpleContexts
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.tac.cg.CHACallGraphKey
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.cg.CallersImplWithOtherCalls
import org.opalj.tac.fpcf.properties.cg.CallersOnlyWithConcreteCallers
import org.opalj.tac.fpcf.properties.cg.NoCallers
import org.opalj.tac.fpcf.properties.cg.OnlyCallersWithUnknownContext
import org.opalj.tac.fpcf.properties.cg.OnlyVMCallersAndWithUnknownContext
import org.opalj.tac.fpcf.properties.cg.OnlyVMLevelCallers

@RunWith(classOf[JUnitRunner])
class CallersTest extends AnyFlatSpec with Matchers {
    val typesProject: SomeProject =
        Project(
            ClassFiles(locateTestResources("classhierarchy.jar", "bi")),
            Iterable.empty,
            libraryClassFilesAreInterfacesOnly = true
        )

    typesProject.get(CHACallGraphKey)
    implicit val declaredMethods: DeclaredMethods = typesProject.get(DeclaredMethodsKey)
    val simpleContexts: SimpleContexts = typesProject.get(SimpleContextsKey)
    implicit val typeProvider: TypeProvider = typesProject.get(TypeProviderKey)

    val declaredMethod: DeclaredMethod = declaredMethods.declaredMethods.find(_ => true).get
    val otherMethod: DeclaredMethod = declaredMethods.declaredMethods.find(_ ne declaredMethod).get

    behavior of "the no caller object"

    it should "update correctly" in {
        assert(NoCallers.updatedWithVMLevelCall() eq OnlyVMLevelCallers)
        assert(NoCallers.updatedWithUnknownContext() eq OnlyCallersWithUnknownContext)
        val oneCaller = NoCallers.updated(
            simpleContexts(otherMethod), simpleContexts(declaredMethod), pc = 0, isDirect = true
        )
        assert(oneCaller.isInstanceOf[CallersOnlyWithConcreteCallers])
        assert(oneCaller.callers(otherMethod).iterator.size == 1 && oneCaller.callers(otherMethod).iterator.exists {
            case (dm, pc, isDirect) => (dm eq declaredMethod) && (pc == 0) && isDirect
        })

    }

    it should "behave correctly" in {
        assert(NoCallers.size == 0)
        assert(NoCallers.callers(otherMethod).iterator.isEmpty)
        assert(!NoCallers.hasVMLevelCallers)
        assert(!NoCallers.hasCallersWithUnknownContext)
    }

    behavior of "only vm level callers"

    it should "update correctly" in {
        assert(OnlyVMLevelCallers.updatedWithVMLevelCall() eq OnlyVMLevelCallers)
        assert(OnlyVMLevelCallers.updatedWithUnknownContext() eq OnlyVMCallersAndWithUnknownContext)
        val oneCaller = OnlyVMLevelCallers.updated(
            simpleContexts(otherMethod), simpleContexts(declaredMethod), pc = 0, isDirect = true
        )
        assert(oneCaller.isInstanceOf[CallersImplWithOtherCalls])
        assert(oneCaller.callers(otherMethod).iterator.size == 1 && oneCaller.callers(otherMethod).iterator.exists {
            case (dm, pc, isDirect) => (dm eq declaredMethod) && (pc == 0) && isDirect
        })
        assert(oneCaller.hasVMLevelCallers)
        assert(!oneCaller.hasCallersWithUnknownContext)
    }

    it should "behave correctly" in {
        assert(OnlyVMLevelCallers.size == 0)
        assert(OnlyVMLevelCallers.callers(otherMethod).iterator.isEmpty)
        assert(OnlyVMLevelCallers.hasVMLevelCallers)
        assert(!OnlyVMLevelCallers.hasCallersWithUnknownContext)
    }

    behavior of "only unknown callers"

    it should "update correctly" in {
        assert(OnlyCallersWithUnknownContext.updatedWithVMLevelCall() eq OnlyVMCallersAndWithUnknownContext)
        assert(OnlyCallersWithUnknownContext.updatedWithUnknownContext() eq OnlyCallersWithUnknownContext)
        val oneCaller = OnlyCallersWithUnknownContext.updated(
            simpleContexts(otherMethod), simpleContexts(declaredMethod), pc = 0, isDirect = true
        )
        assert(oneCaller.isInstanceOf[CallersImplWithOtherCalls])
        assert(oneCaller.callers(otherMethod).iterator.size == 1 && oneCaller.callers(otherMethod).iterator.exists {
            case (dm, pc, isDirect) => (dm eq declaredMethod) && (pc == 0) && isDirect
        })
        assert(!oneCaller.hasVMLevelCallers)
        assert(oneCaller.hasCallersWithUnknownContext)
    }

    it should "behave correctly" in {
        assert(OnlyCallersWithUnknownContext.size == 0)
        assert(OnlyCallersWithUnknownContext.callers(otherMethod).iterator.isEmpty)
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
        val oneCaller = OnlyVMCallersAndWithUnknownContext.updated(
            simpleContexts(otherMethod), simpleContexts(declaredMethod), pc = 0, isDirect = true
        )
        assert(oneCaller.isInstanceOf[CallersImplWithOtherCalls])
        assert(oneCaller.callers(otherMethod).iterator.size == 1 && oneCaller.callers(otherMethod).iterator.exists {
            case (dm, pc, isDirect) => (dm eq declaredMethod) && (pc == 0) && isDirect
        })
        assert(oneCaller.hasVMLevelCallers)
        assert(oneCaller.hasCallersWithUnknownContext)
    }

    it should "behave correctly" in {
        assert(OnlyVMCallersAndWithUnknownContext.size == 0)
        assert(OnlyVMCallersAndWithUnknownContext.callers(otherMethod).iterator.isEmpty)
        assert(OnlyVMCallersAndWithUnknownContext.hasVMLevelCallers)
        assert(OnlyVMCallersAndWithUnknownContext.hasCallersWithUnknownContext)
    }

    behavior of "only with concrete callers"

    it should "update correctly" in {
        val callers = NoCallers.updated(
            simpleContexts(otherMethod), simpleContexts(declaredMethod), pc = 0, isDirect = true
        )

        val withTwoCallers = callers.updated(
            simpleContexts(otherMethod), simpleContexts(otherMethod), pc = 1, isDirect = true
        )
        assert(withTwoCallers.isInstanceOf[CallersOnlyWithConcreteCallers])
        assert(withTwoCallers.size == 2)

        val updateWithSame = callers.updated(
            simpleContexts(otherMethod), simpleContexts(declaredMethod), pc = 0, isDirect = true
        )
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
        val callers = NoCallers.updated(
            simpleContexts(otherMethod), simpleContexts(declaredMethod), pc = 0, isDirect = true
        )
        assert(callers.size == 1)
        assert(callers.callers(otherMethod).iterator.exists {
            case (dm, pc, isDirect) => (dm eq declaredMethod) && (pc == 0) && isDirect
        })
        assert(!callers.hasCallersWithUnknownContext)
        assert(!callers.hasVMLevelCallers)
    }

    behavior of "any kind of callers"

    it should "update correctly" in {
        val callersWithVMLevelCall = OnlyVMLevelCallers.updated(
            simpleContexts(otherMethod), simpleContexts(declaredMethod), pc = 0, isDirect = true
        )
        assert(callersWithVMLevelCall.updatedWithVMLevelCall() eq callersWithVMLevelCall)
        val callersWithBothUnknownCalls1 = callersWithVMLevelCall.updatedWithUnknownContext()
        assert(callersWithBothUnknownCalls1.hasCallersWithUnknownContext)
        assert(callersWithBothUnknownCalls1.hasVMLevelCallers)
        assert(callersWithBothUnknownCalls1.callers(otherMethod).iterator.size == 1)
        assert(callersWithBothUnknownCalls1.callers(otherMethod).iterator.exists {
            case (dm, pc, isDirect) => (dm eq declaredMethod) && (pc == 0) && isDirect
        })

        val callersWithUnknownCallers = OnlyCallersWithUnknownContext.updated(
            simpleContexts(otherMethod), simpleContexts(declaredMethod), pc = 0, isDirect = true
        )
        assert(callersWithUnknownCallers.updatedWithUnknownContext() eq callersWithUnknownCallers)
        val callersWithBothUnknownCalls2 = callersWithUnknownCallers.updatedWithVMLevelCall()
        assert(callersWithBothUnknownCalls2.hasCallersWithUnknownContext)
        assert(callersWithBothUnknownCalls2.hasVMLevelCallers)
        assert(callersWithBothUnknownCalls2.callers(otherMethod).iterator.size == 1)
        assert(callersWithBothUnknownCalls2.callers(otherMethod).iterator.exists {
            case (dm, pc, isDirect) => (dm eq declaredMethod) && (pc == 0) && isDirect
        })

        val twoCallers = callersWithVMLevelCall.updated(
            simpleContexts(otherMethod), simpleContexts(otherMethod), pc = 1, isDirect = true
        )
        assert(twoCallers.size == 2)
        assert(twoCallers.callers(otherMethod).iterator.exists {
            case (dm, pc, isDirect) => (dm eq declaredMethod) && (pc == 0) && isDirect
        })
        assert(twoCallers.callers(otherMethod).iterator.exists {
            case (dm, pc, isDirect) => (dm eq otherMethod) && (pc == 1) && isDirect
        })
    }

    it should "behave correctly" in {
        val encodedCallers = IntMap(
            otherMethod.id ->
                LongLinkedTrieSet(Callers.toLong(declaredMethod.id, pc = 0, isDirect = true))
        )
        val withVM = CallersImplWithOtherCalls(
            encodedCallers, hasVMLevelCallers = true, hasCallersWithUnknownContext = false
        )
        assert(withVM.size == 1)
        assert(withVM.callers(otherMethod).iterator.exists {
            case (dm, pc, isDirect) => (dm eq declaredMethod) && (pc == 0) && isDirect
        })
        assert(!withVM.hasCallersWithUnknownContext)
        assert(withVM.hasVMLevelCallers)

        val withUnknwonContext = CallersImplWithOtherCalls(
            encodedCallers, hasVMLevelCallers = false, hasCallersWithUnknownContext = true
        )
        assert(withUnknwonContext.size == 1)
        assert(withUnknwonContext.callers(otherMethod).iterator.exists {
            case (dm, pc, isDirect) => (dm eq declaredMethod) && (pc == 0) && isDirect
        })
        assert(withUnknwonContext.hasCallersWithUnknownContext)
        assert(!withUnknwonContext.hasVMLevelCallers)

        val withBoth = CallersImplWithOtherCalls(
            encodedCallers, hasVMLevelCallers = true, hasCallersWithUnknownContext = true
        )
        assert(withBoth.size == 1)
        assert(withBoth.callers(otherMethod).iterator.exists {
            case (dm, pc, isDirect) => (dm eq declaredMethod) && (pc == 0) && isDirect
        })
        assert(withBoth.hasCallersWithUnknownContext)
        assert(withBoth.hasVMLevelCallers)
    }
}
