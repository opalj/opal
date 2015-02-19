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
package domain
package l1

import scala.language.existentials
import scala.util.control.Breaks._
import java.net.URL
import scala.Console.BLUE
import scala.Console.BOLD
import scala.Console.GREEN
import scala.Console.RESET
import scala.Iterable
import scala.collection.{ Set, Map }
import scala.collection.parallel.mutable.ParArray
import scala.language.postfixOps
import org.opalj.collection.mutable.Locals
import org.opalj.ai.CorrelationalDomain
import org.opalj.ai.Domain
import org.opalj.ai.InterruptableAI
import org.opalj.ai.IsAReferenceValue
import org.opalj.ai.domain
import org.opalj.ai.ValuesDomain
import org.opalj.br.ClassFile
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.IntLikeType
import org.opalj.br.IntegerType
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet1
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.ai.common.XHTML

/**
 *
 *
 * @author Marius Diebel
 */
object IntegerPairAnalysis extends AnalysisExecutor {

    class AnalysisDomain(
        override val project: Project[java.net.URL],
        val ai: InterruptableAI[_],
        val method: Method)
            extends CorrelationalDomain
            with TheProject
            with TheMethod
            with DefaultDomainValueBinding
            with ThrowAllPotentialExceptionsConfiguration
            with DefaultHandlingOfMethodResults
            with IgnoreSynchronization
            with domain.l0.DefaultTypeLevelFloatValues
            with domain.l0.DefaultTypeLevelDoubleValues
            with domain.l0.TypeLevelFieldAccessInstructions
            with domain.l0.TypeLevelInvokeInstructions
            with SpecialMethodsHandling
            //  with domain.l2.PerformInvocations
            with domain.l1.DefaultReferenceValuesBinding
            with domain.l1.DefaultClassValuesBinding
            with domain.l1.DefaultIntegerRangeValues
            with domain.l1.MaxArrayLengthRefinement
            with domain.l1.ConstraintsBetweenIntegerValues
            with domain.l1.DefaultLongValues
            with domain.l1.LongValuesShiftOperators
            with domain.l1.ConcretePrimitiveValuesConversions
            with domain.DefaultExceptionsFactory
            with domain.ProjectBasedClassHierarchy
            with domain.RecordReturnedValuesInfrastructure
            with domain.RecordAllThrownExceptions
            with TheMemoryLayout {

        type ReturnedValue = List[DomainValue]
        type Exceptions = ThrownException
        type IntRange = IntegerRange

        private[this] var theReturnedValue: List[DomainValue] = null

        def returnedValue: Option[List[DomainValue]] = Option(theReturnedValue)

        protected[this] def doRecordReturnedValue(pc: PC, value: DomainValue): Unit = {

            if (value.isInstanceOf[IntegerRange]) {
                if (theReturnedValue == null)
                    theReturnedValue = List(value)
                else
                    theReturnedValue = theReturnedValue :+ value
            }
        }

        //def invokeExecutionHandler: ((pc: Int, definingClass: org.opalj.br.ClassFile, method: org.opalj.br.Method, operands: List[_158.Value])_158.InvokeExecutionHandler) forSome { val _158: org.opalj.ai.domain.l1.IntegerPairAnalysis.AnalysisDomain } = ???   
        //def isRecursive: ((definingClass: org.opalj.br.ClassFile, method: org.opalj.br.Method, operands: List[_159.Value])Boolean) forSome { val _159: org.opalj.ai.domain.l1.IntegerPairAnalysis.AnalysisDomain } = ???   
        //def shouldInvocationBePerformed(definingClass: org.opalj.br.ClassFile,method: org.opalj.br.Method): Boolean = ??? 

        // some relevant constants
        val positiveVal = IntegerRange(42, 42)
        val negativeVal = IntegerRange(-42, -42)
        val negative = IntegerRange((Int.MinValue + 1), -1)
        val positive = IntegerRange(0, (Int.MaxValue - 1))
        val intMin = IntegerRange((Int.MinValue + 1), (Int.MinValue + 1))
        val intMax = IntegerRange(Int.MaxValue, Int.MaxValue)
        val completeIntegerRange = IntegerRange(Int.MinValue, Int.MaxValue)
        val zero = IntegerRange(0, 0)
        val plusOne = IntegerRange(1, 1)
        val minusOne = IntegerRange(-1, -1)
        val numberOfCalls = 11
    }

    val analysis = new OneStepAnalysis[URL, BasicReport] {

        override def title: String =
            "Analyse Integer-Parameter results"

        override def description: String =
            "Analyse Integer-Parameter results"

        override def doAnalyze(
            theProject: Project[URL],
            parameters: Seq[String],
            isInterrupted: () ⇒ Boolean) = {
            import org.opalj.util.PerformanceEvaluation.{ time, ns2sec }

            val integerPairReturnValues = time {
                for {
                    classFile ← theProject.allProjectClassFiles.par
                    method ← classFile.methods
                    if method.body.isDefined
                    if method.descriptor.parameterTypes.length > 0
                    if method.descriptor.parameterTypes.exists { _.isIntegerType }
                    if method.returnType.isIntegerType
                    originalType = method.returnType
                    parameters = method.parameterTypes
                    ai = new InterruptableAI[Domain]
                    domain = new AnalysisDomain(theProject, ai, method)
                    result = ai(classFile, method, domain)
                } yield {
                    var collectedInformation: List[(AnalysisDomain, (AnalysisDomain#IntRange, AnalysisDomain#IntRange), AnalysisDomain#ReturnedValue, Map[PC, AnalysisDomain#Exceptions])] = null
                    val numberOfIntegerParameters = method.descriptor.parameterTypes.filter(_.isIntegerType).size

                    println("METHOD: "+classFile.thisType.toJava+"{ "+method.toJava+" }")

                    // iterate over the possible permutations of parameter settings
                    for (
                        i ← 1 to domain.numberOfCalls if (i % 2 == 1 || numberOfIntegerParameters % 2 == 1) // in case we have an even number of int parameters we can skip roughly half of the calls
                    ) {
                        val domain = new AnalysisDomain(theProject, ai, method)
                        // get a list of possible permutations for the current parameters, 
                        // note that we drop the last parameter in case the number of parameters is uneven
                        val (intRange1, intRange2) = getCallingParameters(i, domain)
                        println("Parameters: "+intRange1+" , "+intRange2)
                        val listOfParameterPermutations: Iterator[List[domain.IntegerRange]] =
                            if (numberOfIntegerParameters == 1) List(getCallingParameters(i, domain)._1).permutations
                            else List.fill((numberOfIntegerParameters + 1) / 2)(List(intRange1, intRange2)).flatten.drop(numberOfIntegerParameters % 2).permutations
                        for (l ← listOfParameterPermutations) {
                            try {
                                ai.performInterpretation(method.isStrict, method.body.get, domain)(
                                    ai.initialOperands(classFile, method, domain), mapLocals(domain, l, method)(ai.initialLocals(classFile, method, domain)(None)))
                            } catch {
                                case t: Throwable ⇒
                                    val state = XHTML.dump(Some(classFile), Some(method), method.body.get, None, domain)(domain.operandsArray, domain.localsArray)
                                    org.opalj.io.writeAndOpen(state, "crash", ".html")
                                    throw t
                            }
                            if (collectedInformation == null)
                                collectedInformation = List((domain, getCallingParameters(i, domain),
                                    domain.returnedValue.getOrElse(List.empty.asInstanceOf[AnalysisDomain#ReturnedValue]), domain.allThrownExceptions))
                            else collectedInformation = collectedInformation :+ ((domain, getCallingParameters(i, domain),
                                domain.returnedValue.getOrElse(List.empty.asInstanceOf[AnalysisDomain#ReturnedValue]), domain.allThrownExceptions))
                        }
                    }

                    if (collectedInformation.exists(_._3.nonEmpty) || collectedInformation.exists(_._4.nonEmpty)) {
                        val (str, intcount) = createReport(classFile, method, domain)(collectedInformation)
                        IntergerRefinedValue(str, intcount)
                    }
                }
            } { t ⇒ println(f"Analysis time: ${ns2sec(t)}%2.2f seconds.") }

            val d = integerPairReturnValues.filter { _.toString().contains("refined-value") }.size
            println("Number of more refinded-values:  "+d)

            BasicReport(
                integerPairReturnValues.filter { x ⇒ x.isInstanceOf[IntergerRefinedValue] && x.toString().size > 0 }.mkString(
                    "Methods with refined integer return ranges and Exceptions ("+integerPairReturnValues.size+"): \n", "\n", "\n"))
        }

        def getCallingParameters(param: Int, domain: AnalysisDomain): (domain.IntegerRange, domain.IntegerRange) = {
            param match {
                case 1  ⇒ (domain.positive, domain.negative)
                case 2  ⇒ (domain.negative, domain.positive)
                case 3  ⇒ (domain.positiveVal, domain.negativeVal)
                case 4  ⇒ (domain.negativeVal, domain.positiveVal)
                case 5  ⇒ (domain.positive, domain.zero)
                case 6  ⇒ (domain.zero, domain.positive)
                case 7  ⇒ (domain negative, domain.zero)
                case 8  ⇒ (domain.zero, domain.negative)
                case 9  ⇒ (domain.intMax, domain.intMax)
                case 10 ⇒ (domain.intMin, domain.intMin)
                case _  ⇒ (domain.zero, domain.zero)
            }
        }

        def mapLocals(domain: AnalysisDomain, listOfparameters: List[AnalysisDomain#IntRange], method: Method)(defaultLocals: domain.Locals): domain.Locals = {
            var iterator = 0
            for (parameter ← listOfparameters) {
                breakable {
                    for (i ← iterator until defaultLocals.size) {
                        if (defaultLocals(i).isInstanceOf[domain.AnIntegerValue]) {
                            defaultLocals.update(i, parameter.asInstanceOf[domain.IntegerRange])
                            iterator = i + 1
                            break
                        }
                    }
                }
            }
            defaultLocals
        }

        def createReport(classFile: ClassFile,
                         method: Method,
                         domain: AnalysisDomain)(collectedInformation: List[(AnalysisDomain, (AnalysisDomain#IntRange, AnalysisDomain#IntRange), AnalysisDomain#ReturnedValue, Map[PC, AnalysisDomain#Exceptions])]): (String, Int) = {
            import Console._

            def adaptIntRange(targetDomain: AnalysisDomain,
                              intRange: AnalysisDomain#IntRange): targetDomain.IntegerRange = {
                targetDomain.IntegerRange(intRange.lowerBound, intRange.upperBound)
            }
            def adaptExceptions(targetDomain: AnalysisDomain, originDomain: AnalysisDomain,
                                exceptions: Map[PC, AnalysisDomain#Exceptions]): Map[PC, targetDomain.ThrownException] = {
                exceptions.toList.map(listEntry ⇒ (listEntry._1, listEntry._2.map(exception ⇒ exception.asInstanceOf[originDomain.DomainSingleOriginReferenceValue].adapt(
                    targetDomain,
                    exception.asInstanceOf[originDomain.DomainSingleOriginReferenceValue].origin)).toSet)).asInstanceOf[List[(PC, targetDomain.ThrownException)]].toMap
            }

            val declaringClassOfMethod = classFile.thisType.toJava
            // adapt integer result ranges to one domain
            val listOfIntegerRanges = collectedInformation.map(listEntry ⇒
                listEntry._3.map(intVal ⇒
                    adaptIntRange(domain, intVal.asInstanceOf[listEntry._1.IntegerRange]))).flatten.distinct
            var totalReturnRange: domain.IntegerRange = null
            var valuesReport: String = ""
            if (listOfIntegerRanges.nonEmpty) {
                // check whether the method always returns the positive/negative val if its passed a negative and positive one
                val returnsAlwaysValue =
                    if (listOfIntegerRanges.contains(domain.positiveVal) &&
                        listOfIntegerRanges.filter { _.equals(domain.positiveVal) }.size >= method.descriptor.parameterTypes.filter(_.isIntegerType).size)
                        " returns always Positive Val "
                    else if (listOfIntegerRanges.contains(domain.negativeVal) &&
                        listOfIntegerRanges.filter { _.equals(domain.negativeVal) }.size >= method.descriptor.parameterTypes.filter(_.isIntegerType).size)
                        " returns always Negative Val "
                    else ""
                // find lowest lowerbound and highest upperbound of the returned values for this method
                totalReturnRange =
                    domain.IntegerRange((listOfIntegerRanges.reduceLeft((l, r) ⇒ if (r.lowerBound < l.lowerBound) r else l)).lowerBound,
                        (listOfIntegerRanges.reduceLeft((l, r) ⇒ if (r.upperBound > l.upperBound) r else l)).upperBound)
                val conclusion =
                    totalReturnRange match {
                        case domain.zero                 ⇒ " returns always zero"
                        case domain.completeIntegerRange ⇒ " returns the complete integer range"
                        case domain.plusOne              ⇒ " returns always One"
                        case domain.minusOne             ⇒ " returns always minus One"
                        case domain.positive             ⇒ " returns always positive IntegerRange"
                        case domain.negative             ⇒ " returns always negative IntegerRange"
                        case _                           ⇒ ""
                    }
                valuesReport = "INTEGER-Refined return type of "+BOLD + BLUE +
                    declaringClassOfMethod+"{ "+method.toJava+" }"+
                    " => "+GREEN + listOfIntegerRanges + BLUE+" possible Return Range: "+totalReturnRange + returnsAlwaysValue + GREEN + conclusion
            }
            // analyzing the thrown exceptions:
            val listOfExceptionRanges: List[domain.IntegerRange] =
                collectedInformation.map(listEntry ⇒
                    if (listEntry._4.size > 0) {
                        if (method.parameterTypes.size > 1) {
                            List(adaptIntRange(domain, listEntry._2._1.asInstanceOf[listEntry._1.IntegerRange]),
                                adaptIntRange(domain, listEntry._2._2.asInstanceOf[listEntry._1.IntegerRange])).asInstanceOf[List[domain.IntegerRange]]
                        } else {
                            List(adaptIntRange(domain, listEntry._2._1.asInstanceOf[listEntry._1.IntegerRange])).asInstanceOf[List[domain.IntegerRange]]
                        }
                    } else List.empty.asInstanceOf[List[domain.IntegerRange]]).flatten
            val listOfExceptions = collectedInformation.map(listEntry ⇒
                if (listEntry._4.nonEmpty) {
                    adaptExceptions(domain, listEntry._1, listEntry._4)
                } else Map.empty.asInstanceOf[Map[PC, domain.ThrownException]])
            val mergedExceptions = listOfExceptions.reduceLeft((l, r) ⇒
                l ++ r.map {
                    case (pc, ex) ⇒ pc -> (ex ++ l.getOrElse(pc, Set.empty.asInstanceOf[domain.ThrownException]))
                })
            // find lowest lowerbound and highest upperbound of the parameters responsible for causing exceptions
            val exceptionRange = if (mergedExceptions.size > 0) {
                domain.IntegerRange((listOfExceptionRanges.reduceLeft((l, r) ⇒ if (r.lowerBound < l.lowerBound) r else l)).lowerBound,
                    (listOfExceptionRanges.reduceLeft((l, r) ⇒ if (r.upperBound > l.upperBound) r else l)).upperBound)
            } else null
            val exceptionRangeConclusion = if (exceptionRange == null && listOfExceptions.nonEmpty) " no exceptions thrown " else " Total Parameter Range responsible for Exceptions:  "+BLUE + exceptionRange + RED+"  Exceptions thrown: "+mergedExceptions
            val summarizedConclusion =
                if (valuesReport.length() == 0) {
                    BOLD + BLUE+"Exceptions for Method: "+declaringClassOfMethod+"{ "+method.toJava+" }"+" => "+RED + exceptionRangeConclusion + RESET
                } else
                    valuesReport + BLUE + exceptionRangeConclusion + RED + RESET
            // (summarizedConclusion, if (exceptionRange != null && exceptionRange != domain.completeIntegerRange && mergedExceptions.size > 0) 1 else 0)

            (summarizedConclusion, if ((listOfIntegerRanges.nonEmpty && totalReturnRange != domain.completeIntegerRange) || (exceptionRange != null && exceptionRange != domain.completeIntegerRange && mergedExceptions.size > 0)) 1 else 0)
        }
    }
}

case class IntergerRefinedValue(str: String, count: Int) {
    val counted = if (count > 0) " refined-value " else ""
    override def toString = {
        str + counted
    }
}
