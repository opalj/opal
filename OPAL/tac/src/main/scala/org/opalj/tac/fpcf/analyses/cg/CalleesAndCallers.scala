/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import scala.collection.immutable.IntMap

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.LongLinkedTrieSet
import org.opalj.fpcf.EPK
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.Property
import org.opalj.value.ValueInformation
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.fpcf.properties.Context
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.cg.CallersOnlyWithConcreteCallers
import org.opalj.tac.fpcf.properties.cg.ConcreteCallees
import org.opalj.tac.fpcf.properties.cg.NoCallees
import org.opalj.tac.fpcf.properties.cg.OnlyVMLevelCallers

/**
 * A convenience class for call graph constructions. Manages direct/indirect calls and incomplete
 * call sites and allows the analyses to retrieve the required [[org.opalj.fpcf.PartialResult]]s for
 * [[Callers]] and [[Callees]].
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 */
sealed trait CalleesAndCallers {
    final def partialResults(
        callerContext: Context, enforceCalleesResult: Boolean = false
    ): IterableOnce[PartialResult[_, _ >: Null <: Property]] =
        if (directCallees.isEmpty && indirectCallees.isEmpty && incompleteCallSites.isEmpty) {
            if (enforceCalleesResult)
                Iterator(partialResultForCallees(callerContext)) ++ partialResultsForCallers
            else
                partialResultsForCallers
        } else
            Iterator(partialResultForCallees(callerContext)) ++ partialResultsForCallers

    protected def directCallees: IntMap[IntTrieSet] = IntMap.empty

    protected def indirectCallees: IntMap[IntTrieSet] = IntMap.empty

    protected def incompleteCallSites: IntTrieSet = IntTrieSet.empty

    protected def parameters: IntMap[IntMap[Seq[Option[(ValueInformation, IntTrieSet)]]]] = IntMap.empty

    protected def receivers: IntMap[IntMap[Option[(ValueInformation, IntTrieSet)]]] = IntMap.empty

    private[this] def partialResultForCallees(
        callerContext: Context
    ): PartialResult[DeclaredMethod, Callees] = {
        PartialResult[DeclaredMethod, Callees](callerContext.method, Callees.key, {
            case InterimUBP(_) if directCallees.isEmpty && indirectCallees.isEmpty && incompleteCallSites.isEmpty =>
                None

            case InterimUBP(ub: Callees) =>
                Some(InterimEUBP(
                    callerContext.method,
                    ub.updateWithCallees(
                        callerContext,
                        directCallees, indirectCallees,
                        incompleteCallSites,
                        receivers, parameters
                    )
                ))

            case _: EPK[_, _] if directCallees.isEmpty && indirectCallees.isEmpty && incompleteCallSites.isEmpty =>
                Some(InterimEUBP(
                    callerContext.method, NoCallees
                ))

            case _: EPK[_, _] =>
                Some(InterimEUBP(
                    callerContext.method,
                    ConcreteCallees(
                        callerContext,
                        directCallees, indirectCallees,
                        incompleteCallSites,
                        receivers, parameters
                    )
                ))

            case r =>
                throw new IllegalStateException(s"unexpected previous result $r")
        })
    }

    protected def partialResultsForCallers: IterableOnce[PartialResult[DeclaredMethod, Callers]] = Iterator.empty
}

trait IncompleteCallSites extends CalleesAndCallers {
    private[this] var _incompleteCallSites = IntTrieSet.empty
    override protected def incompleteCallSites: IntTrieSet = _incompleteCallSites

    private[cg] def addIncompleteCallSite(pc: Int): Unit = _incompleteCallSites += pc
}

trait Calls extends CalleesAndCallers {

    val isDirect: Boolean

    protected def createPartialResultForContext(
        callerContext: Context,
        calleeContext: Context,
        pc:            Int
    ): PartialResult[DeclaredMethod, Callers] = {
        PartialResult[DeclaredMethod, Callers](calleeContext.method, Callers.key, {
            case InterimUBP(ub: Callers) =>
                val newCallers = ub.updated(calleeContext, callerContext, pc, isDirect)
                // here we assert that update returns the identity if there is no change
                if (ub ne newCallers)
                    Some(InterimEUBP(calleeContext.method, newCallers))
                else
                    None

            case _: EPK[_, _] =>
                val set = LongLinkedTrieSet(Callers.toLong(callerContext.id, pc, isDirect))
                Some(InterimEUBP(
                    calleeContext.method,
                    new CallersOnlyWithConcreteCallers(IntMap(calleeContext.id -> set), 1)
                ))

            case r =>
                throw new IllegalStateException(s"unexpected previous result $r")
        })
    }

    protected var _callees: IntMap[IntTrieSet] = IntMap.empty

    private[this] var _partialResultsForCallers: List[PartialResult[DeclaredMethod, Callers]] =
        List.empty

    def addCall(
        callerContext: Context, pc: Int, calleeContext: Context
    ): Unit = {
        val calleeId = calleeContext.id
        val oldCalleesAtPCOpt = _callees.get(pc)
        if (oldCalleesAtPCOpt.isEmpty) {
            _callees = _callees.updated(pc, IntTrieSet(calleeId))
            _partialResultsForCallers ::= createPartialResultForContext(callerContext, calleeContext, pc)
        } else {
            val oldCalleesAtPC = oldCalleesAtPCOpt.get
            val newCalleesAtPC = oldCalleesAtPC + calleeId

            // here we assert that IntSet returns the identity if the element is already contained
            if (newCalleesAtPC ne oldCalleesAtPC) {
                _callees = _callees.updated(pc, newCalleesAtPC)
                _partialResultsForCallers ::= createPartialResultForContext(callerContext, calleeContext, pc)
            }
        }
    }

    override protected def partialResultsForCallers: IterableOnce[PartialResult[DeclaredMethod, Callers]] = {
        _partialResultsForCallers.iterator ++ super.partialResultsForCallers
    }
}

trait DirectCallsBase extends Calls {
    override protected def directCallees: IntMap[IntTrieSet] = _callees

    override val isDirect: Boolean = true
}

trait IndirectCallsBase extends Calls {

    override val isDirect: Boolean = false

    private[this] var _parameters: IntMap[IntMap[Seq[Option[(ValueInformation, IntTrieSet)]]]] =
        IntMap.empty

    override protected def parameters: IntMap[IntMap[Seq[Option[(ValueInformation, IntTrieSet)]]]] =
        _parameters

    override protected def receivers: IntMap[IntMap[Option[(ValueInformation, IntTrieSet)]]] =
        _receivers

    private[this] var _receivers: IntMap[IntMap[Option[(ValueInformation, IntTrieSet)]]] =
        IntMap.empty

    override protected def indirectCallees: IntMap[IntTrieSet] = _callees

    def addCall(
        callerContext: Context,
        pc:            Int,
        calleeContext: Context,
        params:        Seq[Option[(ValueInformation, IntTrieSet)]],
        receiver:      Option[(ValueInformation, IntTrieSet)]
    ): Unit = {
        addCall(callerContext, pc, calleeContext)
        _parameters = _parameters.updated(
            pc,
            _parameters.getOrElse(pc, IntMap.empty).updated(calleeContext.id, params)
        )
        _receivers = _receivers.updated(
            pc,
            _receivers.getOrElse(pc, IntMap.empty).updated(calleeContext.id, receiver)
        )
    }

    def addCallOrFallback(
        callerContext:      Context,
        pc:                 Int,
        callee:             org.opalj.Result[Method],
        callerPackage:      String,
        fallbackType:       ObjectType,
        fallbackName:       String,
        fallbackDescriptor: MethodDescriptor,
        parameters:         Seq[Option[(ValueInformation, IntTrieSet)]],
        receiver:           Option[(ValueInformation, IntTrieSet)],
        expandContext:      DeclaredMethod => Context
    )(implicit declaredMethods: DeclaredMethods): Unit = {
        if (callee.hasValue) {
            addCall(
                callerContext,
                pc,
                expandContext(declaredMethods(callee.value)),
                parameters,
                receiver
            )
        } else {
            val fallbackCallee = declaredMethods(
                fallbackType,
                callerPackage,
                fallbackType,
                fallbackName,
                fallbackDescriptor
            )
            addCall(callerContext, pc, expandContext(fallbackCallee), parameters, receiver)
        }
    }
}

trait VMReachableMethodsBase extends CalleesAndCallers {
    private[this] var vmReachableMethods: Set[DeclaredMethod] = Set.empty

    def addVMReachableMethod(declaredMethod: DeclaredMethod): Unit =
        vmReachableMethods += declaredMethod

    override protected def partialResultsForCallers: IterableOnce[PartialResult[DeclaredMethod, Callers]] = {
        vmReachableMethods.iterator.map { m =>
            PartialResult[DeclaredMethod, Callers](m, Callers.key, {
                case _: EPK[_, _] =>
                    Some(InterimEUBP(m, OnlyVMLevelCallers))

                case InterimUBP(ub: Callers) =>
                    if (ub.hasVMLevelCallers)
                        None
                    else
                        Some(InterimEUBP(m, ub.updatedWithVMLevelCall()))

                case r =>
                    throw new IllegalStateException(s"unexpected previous result $r")

            })
        } ++ super.partialResultsForCallers
    }
}

class VMReachableMethods extends VMReachableMethodsBase with IncompleteCallSites
class DirectCalls extends DirectCallsBase with IncompleteCallSites
class IndirectCalls extends IndirectCallsBase with IncompleteCallSites
