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
package analyses
// TODO @Dominik please fix package structure

import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.properties.LBDPure
import org.opalj.fpcf.properties.Purity
import org.opalj.fpcf.properties.LBPure
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.DUVar
import org.opalj.tac.Expr
import org.opalj.tac.GetStatic
import org.opalj.tac.Stmt

/**
 * Rates, whether three address code statements perform actions that are domain-specific pure.
 *
 * @author Dominik Helm
 */
trait DomainSpecificRater {
    type V = DUVar[(Domain with RecordDefUse)#DomainValue]

    /**
     * Rates all types of calls.
     */
    def handleCall(call: Call[V], receiver: Option[Expr[V]])(implicit project: SomeProject, code: Array[Stmt[V]]): Option[Purity]

    /**
     * Rates GetStatic expressions.
     */
    def handleGetStatic(expr: GetStatic)(implicit project: SomeProject, code: Array[Stmt[V]]): Option[Purity]

    /**
     * Rates implicit VM exceptions
     */
    def handleException(stmt: Stmt[V]): Option[Purity]
}

/**
 * Default implementation of a rater that handles the use of System.out/System.err and logging as
 * well as some exception types typically used to assert program properties.
 */
object SystemOutLoggingAssertionExceptionRater extends BaseDomainSpecificRater
    with SystemOutErrRater with LoggingRater with AssertionExceptionRater

/**
 * Implementation of a rater that handles the use of System.out/System.err and logging as well as
 * all exceptions
 */
object SystemOutLoggingAllExceptionRater extends BaseDomainSpecificRater
    with SystemOutErrRater with LoggingRater with ExceptionRater

/**
 * Basic rater that does nothing.
 */
class BaseDomainSpecificRater extends DomainSpecificRater {
    def handleCall(call: Call[V], receiver: Option[Expr[V]])(implicit project: SomeProject, code: Array[Stmt[V]]): Option[Purity] = {
        None
    }

    def handleGetStatic(expr: GetStatic)(implicit project: SomeProject, code: Array[Stmt[V]]): Option[Purity] = {
        None
    }

    def handleException(stmt: Stmt[V]): Option[Purity] = {
        None
    }
}

/**
 * Mixin that rates whether a call or GetStatic is part of using `System.out` or `System.err`
 */
trait SystemOutErrRater extends DomainSpecificRater {
    private final val printStream = ObjectType("java/io/PrintStream")

    abstract override def handleCall(call: Call[V], receiver: Option[Expr[V]])(implicit project: SomeProject, code: Array[Stmt[V]]): Option[Purity] = {
        if (receiver.isDefined && call.declaringClass == printStream && isOutErr(receiver.get))
            Some(LBDPure)
        else super.handleCall(call, receiver)
    }

    abstract override def handleGetStatic(expr: GetStatic)(implicit project: SomeProject, code: Array[Stmt[V]]): Option[Purity] = {
        val GetStatic(_, declaringClass, name, _) = expr
        if (declaringClass == ObjectType.System && (name == "out" || name == "err"))
            Some(LBPure)
        else super.handleGetStatic(expr)
    }

    abstract override def handleException(stmt: Stmt[V]): Option[Purity] = {
        super.handleException(stmt)
    }

    private def isOutErr(expr: Expr[V])(implicit code: Array[Stmt[V]]): Boolean = {
        expr.isVar && expr.asVar.definedBy.forall { defSite ⇒
            if (defSite < 0) false
            else {
                val stmt = code(defSite)
                assert(stmt.astID == Assignment.ASTID, "defSite should be assignment")
                if (stmt.asAssignment.expr.astID != GetStatic.ASTID) false
                else {
                    val GetStatic(_, declaringClass, name, _) = stmt.asAssignment.expr
                    declaringClass == ObjectType.System && (name == "out" || name == "err")
                }
            }
        }
    }
}

/**
 * Mixin that rates whether a call is part of using logging.
 */
trait LoggingRater extends DomainSpecificRater {

    private final val loggers = Set(
        ObjectType("org/apache/logging/log4j/LogManager"),
        ObjectType("org/apache/logging/log4j/Logger"),
        ObjectType("org/slf4j/LoggerFactory"),
        ObjectType("org/slf4j/Log"),
        ObjectType("java/util/logging/LogManager"),
        ObjectType("java/util/logging/Logger"),
        ObjectType("org/pmw/tinylog/Logger")
    )

    private final val logLevels = Set(
        ObjectType("org/apache/logging/log4j/Level"),
        ObjectType("java/util/logging/Level")
    )

    abstract override def handleCall(call: Call[V], receiver: Option[Expr[V]])(implicit project: SomeProject, code: Array[Stmt[V]]): Option[Purity] = {
        if (call.declaringClass.isObjectType) {
            val declClass = call.declaringClass.asObjectType
            if (loggers.exists(declClass.isSubtypeOf(_)(project.classHierarchy).isYes))
                Some(LBDPure)
            else super.handleCall(call, receiver)
        } else super.handleCall(call, receiver)
    }

    abstract override def handleGetStatic(expr: GetStatic)(implicit project: SomeProject, code: Array[Stmt[V]]): Option[Purity] = {
        val GetStatic(_, declaringClass, _, _) = expr
        if (logLevels.exists(declaringClass == _))
            Some(LBDPure)
        else super.handleGetStatic(expr)
    }
}

/**
 * Mixin that treats all exceptions as domain-specific.
 */
trait ExceptionRater extends DomainSpecificRater {
    abstract override def handleCall(call: Call[V], receiver: Option[Expr[V]])(implicit project: SomeProject, code: Array[Stmt[V]]): Option[Purity] = {
        implicit val classHierarchy = project.classHierarchy
        val declClass = call.declaringClass
        if (declClass.isObjectType && call.name == "<init>" &&
            declClass.asObjectType.isSubtypeOf(ObjectType.Throwable).isYes &&
            !project.instanceMethods(declClass.asObjectType).exists { mdc ⇒
                mdc.name == "fillInStackTrace" &&
                    mdc.method.classFile.thisType != ObjectType.Throwable
            })
            Some(LBDPure)
        else super.handleCall(call, receiver)
    }

    abstract override def handleGetStatic(expr: GetStatic)(implicit project: SomeProject, code: Array[Stmt[V]]): Option[Purity] = {
        super.handleGetStatic(expr)
    }

    override def handleException(stmt: Stmt[V]): Option[Purity] = {
        Some(LBDPure)
    }
}

/**
 * Mixin that treats some exception types typically used to assert program properties as
 * domain-specific.
 */
trait AssertionExceptionRater extends DomainSpecificRater {

    private final val exceptionTypes = Set(
        ObjectType("java/lang/AssertionError"),
        ObjectType("java/lang/IllegalArgumentException"),
        ObjectType("java/lang/IllegalStateException")
    )

    abstract override def handleCall(call: Call[V], receiver: Option[Expr[V]])(implicit project: SomeProject, code: Array[Stmt[V]]): Option[Purity] = {
        implicit val classHierarchy = project.classHierarchy;
        if (call.declaringClass.isObjectType && call.name == "<init>" &&
            exceptionTypes.exists(call.declaringClass.asObjectType.isSubtypeOf(_).isYes))
            Some(LBDPure)
        else super.handleCall(call, receiver)
    }

    abstract override def handleGetStatic(expr: GetStatic)(implicit project: SomeProject, code: Array[Stmt[V]]): Option[Purity] = {
        super.handleGetStatic(expr)
    }

    override def handleException(stmt: Stmt[V]): Option[Purity] = {
        None
    }
}
