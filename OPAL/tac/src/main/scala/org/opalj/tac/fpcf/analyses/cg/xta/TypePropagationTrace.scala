/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import scala.annotation.elidable

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.time.Instant

import scala.collection.mutable

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.DefinedMethod
import org.opalj.br.ReferenceType
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.br.DeclaredMethod
import org.opalj.br.fpcf.properties.Context
import org.opalj.tac.fpcf.analyses.cg.xta.TypePropagationTrace.Trace

/**
 * This is used in [[TypePropagationAnalysis]] and logs all individual steps of the type propagation
 * as a trace. A simplified textual trace can also be output if the setting "WriteTextualTrace" is
 * set to true. This may be helpful for debugging.
 *
 * All tracing methods called by the [[TypePropagationTrace]] are marked elidable, meaning that the
 * compiler will remove them unless assertions are turned on. This is to avoid performance overhead
 * in cases where tracing is not needed.
 *
 * @author Andreas Bauer
 */
private[xta] class TypePropagationTrace {
    // Textual trace
    private val _out =
        if (TypePropagationTrace.WriteTextualTrace) {
            val file = new FileOutputStream(new File(s"trace${Instant.now.getEpochSecond}.txt"))
            new PrintWriter(file)
        } else {
            null
        }

    // Structural trace (for further evaluation)
    val _trace = Trace(mutable.ArrayBuffer())
    TypePropagationTrace.LastTrace = _trace

    private def traceMsg(msg: String): Unit = {
        if (TypePropagationTrace.WriteTextualTrace) {
            _out.println(msg)
            _out.flush()
        }
    }

    private def simplifiedName(e: Any): String = e match {
        case defM: DefinedMethod => s"${simplifiedName(defM.declaringClassType)}.${defM.name}(...)"
        case rt: ReferenceType   => rt.toJava.substring(rt.toJava.lastIndexOf('.') + 1)
        case _                   => e.toString
    }

    @elidable(elidable.ASSERTION)
    def traceInit(
        method: DefinedMethod
    )(implicit ps: PropertyStore, typeProvider: TypeProvider): Unit = {
        val initialTypes = {
            val typeEOptP = ps(method, InstantiatedTypes.key)
            if (typeEOptP.hasUBP) typeEOptP.ub.types
            else UIDSet.empty[ReferenceType]
        }
        val initialCallees = {
            val calleesEOptP = ps(method, Callees.key)
            if (calleesEOptP.hasUBP)
                calleesEOptP.ub.callSites(typeProvider.newContext(method)).flatMap(_._2)
            else Iterator.empty
        }
        traceMsg(s"init: ${simplifiedName(method)} (initial types: {${initialTypes.map(simplifiedName).mkString(", ")}}, initial callees: {${initialCallees.map(simplifiedName).mkString(", ")}})")
        _trace.events += TypePropagationTrace.Init(method, initialTypes, initialCallees.toSet)
    }

    @elidable(elidable.ASSERTION)
    def traceCalleesUpdate(receiver: DefinedMethod): Unit = {
        traceMsg(s"callee property update: ${simplifiedName(receiver)}")
        _trace.events += TypePropagationTrace.CalleesUpdate(receiver)
    }

    @elidable(elidable.ASSERTION)
    def traceTypeUpdate(receiver: DeclaredMethod, source: Entity, types: UIDSet[ReferenceType]): Unit = {
        traceMsg(s"type set update: for ${simplifiedName(receiver)}, from ${simplifiedName(source)}, with types: {${types.map(simplifiedName).mkString(", ")}}")
        _trace.events += TypePropagationTrace.TypeSetUpdate(receiver, source, types)
    }

    @elidable(elidable.ASSERTION)
    def traceTypePropagation(targetEntity: Entity, propagatedTypes: UIDSet[ReferenceType]): Unit = {
        traceMsg(s"propagate {${propagatedTypes.map(simplifiedName).mkString(", ")}} to ${simplifiedName(targetEntity)}")
        _trace.events.last.typePropagations += TypePropagationTrace.TypePropagation(targetEntity, propagatedTypes)
    }
}

object TypePropagationTrace {
    case class TypePropagation(targetEntity: Entity, types: UIDSet[ReferenceType])
    case class Trace(events: mutable.ArrayBuffer[Event])
    trait Event {
        val typePropagations: mutable.ArrayBuffer[TypePropagation] = new mutable.ArrayBuffer[TypePropagation]()
    }
    case class Init(method: DefinedMethod, initialTypes: UIDSet[ReferenceType], initialCallees: Set[Context]) extends Event
    trait UpdateEvent extends Event
    case class TypeSetUpdate(receiver: Entity, source: Entity, sourceTypes: UIDSet[ReferenceType]) extends UpdateEvent
    case class CalleesUpdate(receiver: Entity) extends UpdateEvent

    // Global variable holding the type propagation trace of the last executed XTA analysis.
    var LastTrace: Trace = _
    var WriteTextualTrace: Boolean = false

    // Tracing and assert are on the same level of elision. Thus, if assertions are turned on, the tracing is also
    // turned on.
    def isEnabled: Boolean = {
        try {
            assert(false)
            false
        } catch {
            case _: AssertionError => true
        }
    }
}