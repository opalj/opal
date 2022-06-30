/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package purity

import org.opalj.value.ValueInformation
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.DPure
import org.opalj.br.fpcf.properties.Pure
import org.opalj.br.fpcf.properties.Purity
import org.opalj.br.MethodDescriptor

/**
 * Rates, whether three address code statements perform actions that are domain-specific pure.
 *
 * @author Dominik Helm
 */
trait DomainSpecificRater {

    type V = DUVar[ValueInformation]

    /**
     * Rates all types of calls.
     */
    def handleCall(
        call:     Call[V],
        receiver: Option[Expr[V]]
    )(
        implicit
        project: SomeProject, code: Array[Stmt[V]]
    ): Option[Purity]

    /**
     * Rates GetStatic expressions.
     */
    def handleGetStatic(expr: GetStatic)(
        implicit
        project: SomeProject, code: Array[Stmt[V]]
    ): Option[Purity]

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
    def handleCall(
        call:     Call[V],
        receiver: Option[Expr[V]]
    )(
        implicit
        project: SomeProject,
        code:    Array[Stmt[V]]
    ): Option[Purity] = {
        None
    }

    def handleGetStatic(
        expr: GetStatic
    )(
        implicit
        project: SomeProject, code: Array[Stmt[V]]
    ): Option[Purity] = {
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

    abstract override def handleCall(
        call:     Call[V],
        receiver: Option[Expr[V]]
    )(
        implicit
        project: SomeProject, code: Array[Stmt[V]]
    ): Option[Purity] = {
        if (receiver.isDefined && call.declaringClass == printStream && isOutErr(receiver.get))
            Some(DPure)
        else super.handleCall(call, receiver)
    }

    abstract override def handleGetStatic(
        expr: GetStatic
    )(
        implicit
        project: SomeProject,
        code:    Array[Stmt[V]]
    ): Option[Purity] = {
        val GetStatic(_, declaringClass, name, _) = expr
        if (declaringClass == ObjectType.System && (name == "out" || name == "err"))
            Some(Pure)
        else super.handleGetStatic(expr)
    }

    abstract override def handleException(stmt: Stmt[V]): Option[Purity] = {
        super.handleException(stmt)
    }

    private def isOutErr(expr: Expr[V])(implicit code: Array[Stmt[V]]): Boolean = {
        expr.isVar && expr.asVar.definedBy.forall { defSite =>
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

    abstract override def handleCall(
        call: Call[V], receiver: Option[Expr[V]]
    )(
        implicit
        project: SomeProject,
        code:    Array[Stmt[V]]
    ): Option[Purity] = {
        if (call.declaringClass.isObjectType) {
            val declClass = call.declaringClass.asObjectType
            if (loggers.exists(declClass.isSubtypeOf(_)(project.classHierarchy)))
                Some(DPure)
            else super.handleCall(call, receiver)
        } else super.handleCall(call, receiver)
    }

    abstract override def handleGetStatic(
        expr: GetStatic
    )(
        implicit
        project: SomeProject,
        code:    Array[Stmt[V]]
    ): Option[Purity] = {
        val GetStatic(_, declaringClass, _, _) = expr
        if (logLevels.exists(declaringClass == _))
            Some(DPure)
        else super.handleGetStatic(expr)
    }
}

/**
 * Mixin that treats all exceptions as domain-specific.
 */
trait ExceptionRater extends DomainSpecificRater {
    abstract override def handleCall(
        call:     Call[V],
        receiver: Option[Expr[V]]
    )(
        implicit
        project: SomeProject,
        code:    Array[Stmt[V]]
    ): Option[Purity] = {
        implicit val classHierarchy = project.classHierarchy
        val declClass = call.declaringClass
        if (declClass.isObjectType && call.name == "<init>" &&
            declClass.asObjectType.isSubtypeOf(ObjectType.Throwable) &&
            !org.opalj.control.find(project.instanceMethods(declClass.asObjectType))(mdc =>
                mdc.method.compare(
                    "fillInStackTrace",
                    MethodDescriptor.withNoArgs(ObjectType.Throwable)
                )).exists(_.method.classFile.thisType != ObjectType.Throwable))
            Some(DPure)
        else super.handleCall(call, receiver)
    }

    abstract override def handleGetStatic(
        expr: GetStatic
    )(
        implicit
        project: SomeProject,
        code:    Array[Stmt[V]]
    ): Option[Purity] = {
        super.handleGetStatic(expr)
    }

    override def handleException(stmt: Stmt[V]): Option[Purity] = {
        Some(DPure)
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

    abstract override def handleCall(
        call:     Call[V],
        receiver: Option[Expr[V]]
    )(
        implicit
        project: SomeProject, code: Array[Stmt[V]]
    ): Option[Purity] = {
        implicit val classHierarchy = project.classHierarchy;
        if (call.declaringClass.isObjectType && call.name == "<init>" &&
            exceptionTypes.exists(call.declaringClass.asObjectType.isSubtypeOf))
            Some(DPure)
        else super.handleCall(call, receiver)
    }

    abstract override def handleGetStatic(
        expr: GetStatic
    )(
        implicit
        project: SomeProject,
        code:    Array[Stmt[V]]
    ): Option[Purity] = {
        super.handleGetStatic(expr)
    }

    override def handleException(stmt: Stmt[V]): Option[Purity] = {
        None
    }
}
