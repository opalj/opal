/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

import scala.collection.immutable.IntMap

import org.opalj.collection.immutable.LongTrieSet
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.cg.properties.CallersOnlyWithConcreteCallers
import org.opalj.fpcf.cg.properties.CallersProperty
import org.opalj.value.ValueInformation
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods

private[cg] class CalleesAndCallers(
        //IMPROVE: mutable map for performance
        private[this] var _callees: IntMap[IntTrieSet] = IntMap.empty
) {

    private[this] var _incompleteCallsites: IntTrieSet = IntTrieSet.empty

    private[this] var _partialResultsForCallers: List[PartialResult[DeclaredMethod, CallersProperty]] =
        List.empty

    private[cg] def callees: IntMap[IntTrieSet] = _callees

    private[cg] def partialResultsForCallers: List[PartialResult[DeclaredMethod, CallersProperty]] = {
        _partialResultsForCallers
    }

    private[cg] def clearPartialResultsForCallers(): Unit = {
        _partialResultsForCallers = Nil
    }

    private[cg] def incompleteCallsites: IntTrieSet = _incompleteCallsites

    private[cg] def addIncompleteCallsite(pc: Int): Unit = _incompleteCallsites += pc

    private[cg] def updateWithCall(
        caller: DeclaredMethod, callee: DeclaredMethod, pc: Int
    ): Unit = {
        val calleeId = callee.id
        // todo here we could slightly improve the performance by omitting the latter contains check
        if (!_callees.contains(pc) || !_callees(pc).contains(calleeId)) {
            _callees = _callees.updated(pc, _callees.getOrElse(pc, IntTrieSet.empty) + calleeId)
            _partialResultsForCallers ::= createPartialResultForCaller(caller, callee, pc)
        }
    }

    def updateWithCallOrFallback(
        caller:             DeclaredMethod,
        callee:             org.opalj.Result[Method],
        pc:                 Int,
        callerPackage:      String,
        fallbackType:       ObjectType,
        fallbackName:       String,
        fallbackDescriptor: MethodDescriptor
    )(implicit declaredMethods: DeclaredMethods): Unit = {
        if (callee.hasValue) {
            updateWithCall(caller, declaredMethods(callee.value), pc)
        } else {
            val fallbackCallee = declaredMethods(
                fallbackType,
                callerPackage,
                fallbackType,
                fallbackName,
                fallbackDescriptor
            )
            updateWithCall(caller, fallbackCallee, pc)

        }
    }

    private[this] def createPartialResultForCaller(
        caller: DeclaredMethod, callee: DeclaredMethod, pc: Int
    ): PartialResult[DeclaredMethod, CallersProperty] = {
        PartialResult[DeclaredMethod, CallersProperty](callee, CallersProperty.key, {
            case InterimUBP(ub) ⇒
                val newCallers = ub.updated(caller, pc)
                // here we assert that update returns the identity if there is no change
                if (ub ne newCallers)
                    Some(InterimEUBP(callee, newCallers))
                else
                    None

            case _: EPK[_, _] ⇒
                val set = LongTrieSet(CallersProperty.toLong(caller.id, pc))
                Some(InterimEUBP(
                    callee,
                    new CallersOnlyWithConcreteCallers(set)
                ))

            case r ⇒
                throw new IllegalStateException(s"unexpected previous result $r")
        })
    }
}

private[cg] class IndirectCalleesAndCallers(
        _callees:                      IntMap[IntTrieSet]                                                       = IntMap.empty,
        private[this] var _parameters: IntMap[Map[DeclaredMethod, Seq[Option[(ValueInformation, IntTrieSet)]]]] = IntMap.empty
) extends CalleesAndCallers(_callees) {
    private[cg] def parameters: IntMap[Map[DeclaredMethod, Seq[Option[(ValueInformation, IntTrieSet)]]]] =
        _parameters

    private[cg] override def updateWithCall(
        caller: DeclaredMethod, callee: DeclaredMethod, pc: Int
    ): Unit = {
        throw new UnsupportedOperationException("Use updateWithIndirectCall instead!")
    }

    private[cg] def updateWithIndirectCall(
        caller:     DefinedMethod,
        callee:     DeclaredMethod,
        pc:         Int,
        parameters: Seq[Option[(ValueInformation, IntTrieSet)]]
    ): Unit = {
        super.updateWithCall(caller, callee, pc)
        _parameters = _parameters.updated(
            pc,
            _parameters.getOrElse(pc, Map.empty).updated(callee, parameters)
        )
    }

    def updateWithIndirectCallOrFallback(
        caller:             DefinedMethod,
        callee:             org.opalj.Result[Method],
        pc:                 Int,
        callerPackage:      String,
        fallbackType:       ObjectType,
        fallbackName:       String,
        fallbackDescriptor: MethodDescriptor,
        parameters:         Seq[Option[(ValueInformation, IntTrieSet)]]
    )(implicit declaredMethods: DeclaredMethods): Unit = {
        if (callee.hasValue) {
            updateWithIndirectCall(caller, declaredMethods(callee.value), pc, parameters)
        } else {
            val fallbackCallee = declaredMethods(
                fallbackType,
                callerPackage,
                fallbackType,
                fallbackName,
                fallbackDescriptor
            )
            updateWithIndirectCall(caller, fallbackCallee, pc, parameters)

        }
    }

}