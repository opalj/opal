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

import scala.language.existentials

import java.net.URL

import scala.collection.parallel.immutable.ParIterable

import org.opalj.br.analyses.{ Analysis, AnalysisExecutor, BasicReport, Project, SomeProject }
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.IntegerType
import org.opalj.br.FieldType
import org.opalj.br.ArrayType
import org.opalj.br.ReferenceType
import org.opalj.util.Answer
import org.opalj.ai.project.CallGraph
import org.opalj.br.ClassFile
import org.opalj.ai.domain.MethodCallResults
import org.opalj.ai.domain.l0.RecordMethodCallResults
import org.opalj.ai.domain.l1.ReferenceValues
import org.opalj.ai.domain.l2.CalledMethodsStore

/**
 * Demonstrates how to identify if an integer value that is passed to a native
 * method is always bounded.
 *
 * @author Michael Eichberg
 */
object NativeMethodCallsAnalysis extends AnalysisExecutor {

    class ValuesDomain(
        override val project: Project[java.net.URL])
            extends CoreDomain
            with domain.DefaultDomainValueBinding
            with domain.ThrowAllPotentialExceptionsConfiguration
            with domain.l0.DefaultTypeLevelLongValues
            with domain.l0.DefaultTypeLevelFloatValues
            with domain.l0.DefaultTypeLevelDoubleValues
            with domain.l1.DefaultReferenceValuesBinding
            with domain.l1.DefaultIntegerRangeValues
            with domain.TheProject[java.net.URL]
            with domain.ProjectBasedClassHierarchy
            with TypedValuesFactory

    abstract class AnalysisDomain(
        override val project: Project[java.net.URL],
        val method: Method)
            extends Domain
            with domain.DefaultDomainValueBinding
            with domain.ThrowAllPotentialExceptionsConfiguration
            with domain.l0.DefaultTypeLevelLongValues
            with domain.l0.DefaultTypeLevelFloatValues
            with domain.l0.DefaultTypeLevelDoubleValues
            with domain.l0.TypeLevelFieldAccessInstructions
            with domain.l0.TypeLevelInvokeInstructions
            with domain.l1.DefaultReferenceValuesBinding
            with domain.l1.DefaultIntegerRangeValues
            with domain.l2.PerformInvocationsWithRecursionDetection
            with domain.DefaultHandlingOfMethodResults
            with domain.IgnoreSynchronization
            with domain.TheProject[java.net.URL]
            with domain.TheMethod
            with domain.ProjectBasedClassHierarchy
            with domain.l0.DefaultPrimitiveTypeConversions
            with RecordMethodCallResults { outerDomain ⇒
        /****************** ??? ********/

        //        def invokeExecutionHandler(
        //            pc: PC,
        //            definingClass: ClassFile,
        //            method: Method,
        //            operands: Operands): InvokeExecutionHandler =
        //            new InvokeExecutionHandler {
        //
        //                val domain: Domain with MethodCallResults = new AnalysisDomain(project, method)
        //
        //                def ai: AI[_ >: domain.type] = BaseAI
        //            }

        val calledMethodsStore: CalledMethodsStore

        def invokeExecutionHandler(
            pc: PC,
            definingClass: ClassFile,
            method: Method,
            operands: Operands): InvokeExecutionHandler =
            new InvokeExecutionHandler {

                override val domain =
                    new AnalysisDomain(project, method) {
                        override val calledMethodsStore: AnalysisDomain.this.calledMethodsStore.type =
                            AnalysisDomain.this.calledMethodsStore
                    }

                def ai: AI[_ >: domain.type] = BaseAI
            }

        type Id = String

        def id = "Domain used by the anaylsis CallsOfNativeMethodsWithBoundedValues"

        override def maxSizeOfIntegerRanges: Long = 128l
    }

    val analysis = new Analysis[URL, BasicReport] {

        override def title: String =
            "Calls of native Methods with Bounded Values"

        override def description: String =
            "Identifies calls of native methods with bounded parameters."

        override def analyze(theProject: Project[URL], parameters: Seq[String]) = {
            println("Calculating CallGraph")
            val project.ComputedCallGraph(callGraph, /*we don't care about unresolved methods etc. */ _, _) =
                theProject.get(project.VTACallGraphKey)

            println("Identify native methods with integer or reference parameters")
            val calledNativeMethods: scala.collection.parallel.ParIterable[Method] =
                (theProject.classFiles.par.map { classFile ⇒
                    classFile.methods.filter { m ⇒
                        m.isNative &&
                            (m.descriptor.parameterTypes.exists(_.isIntegerType) || m.descriptor.parameterTypes.exists(_.isReferenceType)) &&
                            /*We want to make some comparisons*/ callGraph.calledBy(m).size > 0 // &&
                        // let's forget about System.arraycopy... causes too much
                        // noise/completely dominates the results
                        // E.g., typical use case: System.arraycopy(A,0,B,0,...)
                        // !(m.name == "arraycopy" && classFile.thisType == ObjectType("java/lang/System"))
                    }
                }).flatten
            println(
                calledNativeMethods.map(_.toJava).toList.sorted.
                    mkString("Called Native Methods ("+calledNativeMethods.size+"):\n", "\n", ""))

            /*prepare int output */
            val mutex = new Object
            var intresults: List[CallWithBoundedMethodParameter] = Nil
            def addIntResult(r: CallWithBoundedMethodParameter) {
                mutex.synchronized { intresults = r :: intresults }
            }
            /* prepare reference type output */
            var referencetyperesults: List[CallWithNullCheckedParameter] = Nil
            def addReferenceResult(r: CallWithNullCheckedParameter) {
                mutex.synchronized { referencetyperesults = r :: referencetyperesults }
            }

            val coordinatingDomain = new ValuesDomain(theProject)
            val theCalledMethodsStore: CalledMethodsStore = new CalledMethodsStore(coordinatingDomain)

            val unboundedCalls = new java.util.concurrent.atomic.AtomicInteger(0)
            for {
                nativeMethod ← calledNativeMethods //<= ParIterables
                // - The last argument to the method is the top-most stack value.
                // - For this analysis, we don't care whether we call a native instance 
                //   or native static method
                parametersCount = nativeMethod.parameterTypes.size
                parameterIndexes = nativeMethod.parameterTypes.zipWithIndex.collect {
                    case (IntegerType | _: ReferenceType, index) ⇒ index //Collect both integertype and reference type
                }

                // For the stack we don't distinguish between computational type
                // one and two category values; hence, no complex mapping is required.
                stackIndexes = parameterIndexes.map((parametersCount - 1) - _) //index of analyzed parameters in parameterStack.
                (caller, pcs) ← callGraph.calledBy(nativeMethod) //iterate through list of callers of current native method.

            } {

                //wenn private, dann zunächst in alle caller gehen und finales result mit zusammengeführten domain values durchführen.
                val domain = new AnalysisDomain(theProject, caller) { val calledMethodsStore = theCalledMethodsStore }
                //     val oDomain = domain
                val callerClassFile = theProject.classFile(caller)

                val result = if (caller.isPrivate) {
                    //collect ALL DomainValues from ALL Callers and merge them.       
                    //                    val coordinatingDomain = new AnalysisDomain(theProject, null,calledMethodsStore) //gemeinsame Domain
                    val adaptedValues = for {
                        (ccaller, pcs) ← callGraph.calledBy(caller).toIterable
                        result = BaseAI(theProject.classFile(ccaller), ccaller, new AnalysisDomain(theProject, ccaller) { val calledMethodsStore = theCalledMethodsStore })
                        pc ← pcs
                    } yield { result.operandsArray(pc).map(_.adapt(coordinatingDomain, Int.MinValue)) }

                    //alternative

                    //                    val lod = for {
                    //                        (ccaller, pcs) ← callGraph.calledBy(caller)
                    //                        result= BaseAI(theProject.classFile(ccaller), caller, oDomain)
                    //                        pc ← pcs
                    //                    } yield result.operandsArray(pc).asInstanceOf[oDomain.Operands]

                    //merge two lists of domain values
                    def mergeDomainValues(domainValues1: List[coordinatingDomain.DomainValue], domainValues2: List[coordinatingDomain.DomainValue]): List[coordinatingDomain.DomainValue] = {
                        domainValues1.zip(domainValues2).zipWithIndex.map { ops ⇒
                            val ((op1, op2), pc) = ops
                            coordinatingDomain.mergeDomainValues(-1 - pc, op1, op2)
                        }
                    }
                    //folding them all together and putting them into the "right" domain of the caller method
                    val mergedDomainValues: Option[IndexedSeq[domain.DomainValue]] = Some(adaptedValues.fold(adaptedValues.head)(mergeDomainValues).map(domainValue ⇒ domainValue.adapt(domain, Int.MinValue)).toIndexedSeq)

                    BaseAI.perform(callerClassFile, caller, domain)(mergedDomainValues)
                } else {

                    BaseAI(callerClassFile, caller, domain) //abstract analysis if caller is not private
                }
                for {
                    pc ← pcs
                    operands = result.operandsArray(pc)
                    if operands != null //<= this is practically the only place where a null check is necessary  
                    stackIndex ← stackIndexes
                } {
                    operands(stackIndex) match {
                        case domain.IntegerRange(lb, ub) ⇒

                            addIntResult(
                                CallWithBoundedMethodParameter(
                                    theProject,
                                    nativeMethod,
                                    parametersCount - stackIndex,
                                    caller,
                                    pc,
                                    lb,
                                    ub))

                        case x: domain.DomainReferenceValue if (x.upperTypeBound.size > 0) && (x.upperTypeBound.first eq ObjectType.String) && (x.isNull.isNo || x.isNull.isYes) ⇒ addReferenceResult( //suche nur nach Strings
                            CallWithNullCheckedParameter(
                                theProject,
                                nativeMethod,
                                parametersCount - stackIndex,
                                caller,
                                pc,
                                x.isNull))

                        case _ ⇒
                            unboundedCalls.incrementAndGet()
                    }
                }
            }
            /**
             * build output report - outputs only the referency type result list for now.
             */
            BasicReport(
                "Unbounded calls: "+unboundedCalls.get+"\n"+
                    referencetyperesults.sortWith((l, r) ⇒ theProject.classFile(l.caller).thisType.id < theProject.classFile(r.caller).thisType.id).
                    mkString("Bounded calls:\n", "\n\n", "\n"))

            /* For Java 8, this analysis will, e.g., report the following:
             * 
             * 430    public int More ...deflate(byte[] b, int off, int len, int flush) {
             * 431        if (b == null) {
             * 432            throw new NullPointerException();
             * 433        }
             * 434        if (off < 0 || len < 0 || off > b.length - len) {
             * 435            throw new ArrayIndexOutOfBoundsException();
             * 436        }
             * 437        synchronized (zsRef) {
             * 438            ensureOpen();
             * 439            if (flush == NO_FLUSH || flush == SYNC_FLUSH ||
             * 440                flush == FULL_FLUSH) {
             * 441                int thisLen = this.len;
             * 442                int n = deflateBytes(zsRef.address(), b, off, len, flush);
             * 443                bytesWritten += n;
             * 444                bytesRead += (thisLen - this.len);
             * 445                return n;
             * 446            }
             * 447            throw new IllegalArgumentException();
             * 448        }
             * 449    }
             * 
             * The method java.util.zip.Deflater{ int deflate(byte[],int,int,int) } calls in line 442 the native method java.util.zip.Deflater{ int deflateBytes(long,byte[],int,int,int) } and passes in as the 5. parameter a bounded value: [0,3].
             * The method java.util.zip.Deflater{ int deflate(byte[],int,int,int) } calls in line 442 the native method java.util.zip.Deflater{ int deflateBytes(long,byte[],int,int,int) } and passes in as the 4. parameter a bounded value: [0,2147483647].
             * The method java.util.zip.Deflater{ int deflate(byte[],int,int,int) } calls in line 442 the native method java.util.zip.Deflater{ int deflateBytes(long,byte[],int,int,int) } and passes in as the 3. parameter a bounded value: [0,2147483647].
             * 
             */
        }
    }

}

case class CallWithNullCheckedParameter(
        project: SomeProject,
        nativeMethod: Method,
        paramterIntex: Int,
        caller: Method,
        callSite: PC,
        isNull: Answer) {

    override def toString = {
        import Console._
        val declaringClassOfNativeMethod = project.classFile(nativeMethod).thisType.toJava
        val declaringClassOfCaller = project.classFile(caller).thisType.toJava

        "The method "+
            BOLD + declaringClassOfCaller+"{ "+caller.toJava+" }"+RESET+
            " calls in line "+caller.body.get.lineNumber(callSite).getOrElse("N/A")+" the native method "+
            BOLD + BLUE + declaringClassOfNativeMethod+"{ "+nativeMethod.toJava+" }"+RESET+
            " with the parameter being Null: "+isNull+"."
    }
}



