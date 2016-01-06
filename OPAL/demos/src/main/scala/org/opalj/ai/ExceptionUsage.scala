/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package ai

import java.net.URL
import java.io.File
import org.opalj.collection.immutable.{UIDSet, UIDSet1}
import org.opalj.br._
import org.opalj.br.analyses._
import org.opalj.br.instructions._

/**
 * Analyses the usage of exceptions in a program.
 *
 * @author Michael Eichberg
 */
object ExceptionUsage extends DefaultOneStepAnalysis {

    override def title: String =
        "Intraprocedural Usage of Exceptions"

    override def description: String =
        "Analyses the usage of exceptions within the scope of a method."

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ) = {

        if (theProject.classFile(ObjectType("java/lang/Object")).isEmpty) {
            Console.err.println(
                "[warn] It seems as if the JDK was not loaded"+
                    "(use: -libcp=<PATH TO THE JRE>); "+
                    "the results of the analysis might not be useful."
            )
        }

        val usages = (for {
            classFile ← theProject.allProjectClassFiles.par
            method @ MethodWithBody(body) ← classFile.methods
            result = BaseAI(classFile, method, new ExceptionUsageAnalysisDomain(theProject, method))
        } yield {
            import scala.collection.mutable._

            def typeName(value: result.domain.DomainSingleOriginReferenceValue): String =
                value.upperTypeBound.map(_.toJava).mkString(" with ")

            val exceptionUsages = Map.empty[(PC, String), Set[UsageKind.Value]]

            // 1. let's collect _all_ definition sites (program counters)
            //    of all exceptions (objects that are an instance of Throwable)
            //    (Since exceptions are never created in a register, it is sufficient
            //    to analyze the operands.)
            def collectExceptions(value: result.domain.DomainSingleOriginReferenceValue): Unit = {
                if (value.isNull.isNoOrUnknown &&
                    value.isValueSubtypeOf(ObjectType.Throwable).isYes) {
                    val key = (value.origin, typeName(value))
                    if (!exceptionUsages.contains(key)) {
                        exceptionUsages += ((key, Set.empty))
                    }
                }
            }

            for {
                operands ← result.operandsArray;
                if (operands ne null)
                operand ← operands
            } {
                operand match {
                    case v: result.domain.SingleOriginReferenceValue ⇒
                        collectExceptions(v)
                    case result.domain.MultipleReferenceValues(singleOriginReferenceValues) ⇒
                        singleOriginReferenceValues.foreach(collectExceptions)
                    case _ ⇒ /*not relevant*/
                }
            }

            def updateUsageKindForValue(
                value:     result.domain.DomainSingleOriginReferenceValue,
                usageKind: UsageKind.Value
            ): Unit = {
                exceptionUsages.get((value.origin, typeName(value))).map(_ += usageKind)
            }

            def updateUsageKind(
                value:     result.domain.DomainValue,
                usageKind: UsageKind.Value
            ): Unit = {
                value match {
                    case v: result.domain.SingleOriginReferenceValue ⇒
                        updateUsageKindForValue(v, usageKind)
                    case result.domain.MultipleReferenceValues(singleOriginReferenceValues) ⇒
                        singleOriginReferenceValues.foreach { v ⇒
                            updateUsageKindForValue(v, usageKind)
                        }
                    case _ ⇒ /*not relevant*/
                }
            }

            body.foreach { (pc, instruction) ⇒
                val operands = result.operandsArray(pc)
                if (operands != null) { // the instruction is reached...
                    instruction match {
                        case ATHROW ⇒
                            updateUsageKind(operands.head, UsageKind.IsThrown)
                        case i: MethodInvocationInstruction ⇒
                            val methodDescriptor = i.methodDescriptor
                            val parametersCount = methodDescriptor.parametersCount
                            operands.take(parametersCount).foreach(updateUsageKind(_, UsageKind.UsedAsParameter))
                            if (i.isVirtualMethodCall) {
                                updateUsageKind(operands.drop(parametersCount).head, UsageKind.UsedAsReceiver)
                            }
                        case i: FieldWriteAccess ⇒
                            updateUsageKind(operands.head, UsageKind.StoredInField)
                        case ARETURN ⇒
                            updateUsageKind(operands.head, UsageKind.IsReturned)
                        case AASTORE ⇒
                            updateUsageKind(operands.head, UsageKind.StoredInArray)
                        case _ ⇒
                        /*nothing to do*/
                    }
                }
            }

            val usages =
                for { (key @ (pc, typeName), exceptionUsage) ← exceptionUsages }
                    yield ExceptionUsage(classFile, method, pc, typeName, exceptionUsage)
 
            if (usages.isEmpty)
                None
            else {
                Some(usages)
            }
        }).filter(_.isDefined).map(_.get).flatten

        val (notUsed, used) = usages.toSeq.partition(_.usageInformation.isEmpty)
        var report = used.map(_.toString).toList.sorted.mkString("\nUsed\n", "\n", "\n")
        report += notUsed.map(_.toString).toList.sorted.mkString("\nNot Used\n", "\n", "\n")

        BasicReport(report)
    }

}

case class ExceptionUsage(
        classFile:        ClassFile,
        method:           Method,
        definitionSite:   PC,
        exceptionType:    String,
        usageInformation: scala.collection.Set[UsageKind.Value]
) extends scala.math.Ordered[ExceptionUsage] {

    override def toString: String = {
        val lineNumber = method.body.get.lineNumber(definitionSite).map("line="+_+";").getOrElse("")
        import Console._
        RED + classFile.thisType.toJava + RESET+"{ "+GREEN + method.toJava + RESET+"{ "+
            BOLD+"["+lineNumber+"pc="+definitionSite+"] "+exceptionType+" => "+
            usageInformation.mkString(", ") + 
            RESET+" }"+" }"
    }

    def compare(that: ExceptionUsage) = this.toString.compare(that.toString)
}

object UsageKind extends Enumeration {
    val UsedAsParameter = UsageKind.Value
    val UsedAsReceiver = UsageKind.Value
    val IsReturned = UsageKind.Value
    val IsThrown = UsageKind.Value
    val StoredInArray = UsageKind.Value
    val StoredInField = UsageKind.Value
}

class ExceptionUsageAnalysisDomain(val project: Project[java.net.URL], val method: Method)
        extends CorrelationalDomain
        with domain.DefaultDomainValueBinding
        with domain.l0.TypeLevelPrimitiveValuesConversions
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l0.DefaultTypeLevelLongValues
        with domain.l0.TypeLevelLongValuesShiftOperators
        with domain.l0.DefaultTypeLevelIntegerValues
        with domain.l0.TypeLevelFieldAccessInstructions
        with domain.l0.TypeLevelInvokeInstructions
        with domain.l1.DefaultReferenceValuesBinding
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization
        with domain.TheProject
        with domain.TheMethod
        with domain.ProjectBasedClassHierarchy {

    def throwAllHandledExceptionsOnMethodCall: Boolean = true

    def throwArithmeticExceptions: Boolean = false

    def throwNullPointerExceptionOnMethodCall: Boolean = false
    def throwNullPointerExceptionOnFieldAccess: Boolean = false
    def throwNullPointerExceptionOnMonitorAccess: Boolean = false
    def throwNullPointerExceptionOnArrayAccess: Boolean = false
    def throwNullPointerExceptionOnThrow: Boolean = false

    def throwIllegalMonitorStateException: Boolean = false

    def throwArrayIndexOutOfBoundsException: Boolean = false
    def throwArrayStoreException: Boolean = false
    def throwNegativeArraySizeException: Boolean = false

    def throwClassCastException: Boolean = false

    def throwClassNotFoundException: Boolean = false
}
