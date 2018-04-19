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
package ai

import java.net.URL

import org.opalj.br.analyses.{DefaultOneStepAnalysis, BasicReport, Project, SomeProject}
import org.opalj.br.Method
import org.opalj.br.{ObjectType, IntegerType}
import org.opalj.ai.analyses.cg.ComputedCallGraph
import org.opalj.ai.analyses.cg.VTACallGraphKey

/**
 * Demonstrates how to identify if an integer value that is passed to a native
 * method is always bounded.
 *
 * @author Michael Eichberg
 */
object CallsOfNativeMethodsWithBoundedValues extends DefaultOneStepAnalysis {

    class AnalysisDomain(
            override val project: Project[java.net.URL],
            val method:           Method
    ) extends CorrelationalDomain
        with domain.DefaultDomainValueBinding
        with domain.ThrowAllPotentialExceptionsConfiguration
        with domain.l1.DefaultIntegerRangeValues
        with domain.l0.DefaultTypeLevelLongValues
        with domain.l0.TypeLevelPrimitiveValuesConversions
        with domain.l0.TypeLevelLongValuesShiftOperators
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l1.DefaultReferenceValuesBinding
        with domain.l0.TypeLevelFieldAccessInstructions
        with domain.l0.TypeLevelInvokeInstructions
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization
        with domain.TheProject
        with domain.TheMethod {

        override def maxCardinalityOfIntegerRanges: Long = 128L
    }

    override def title: String =
        "Calls of native Methods with Bounded Integer Values"

    override def description: String =
        "Identifies calls of native methods with bounded integer parameters."

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        println("Calculating CallGraph")
        val ComputedCallGraph(callGraph, /*we don't care about unresolved methods etc. */ _, _) =
            theProject.get(VTACallGraphKey)

        println("Identify native methods with integer parameters")
        val calledNativeMethods: scala.collection.parallel.ParIterable[Method] =
            theProject.allClassFiles.par.flatMap { classFile ⇒
                classFile.methods.filter { m ⇒
                    m.isNative &&
                        m.descriptor.parameterTypes.exists(_.isIntegerType) &&
                        /*We want to make some comparisons*/ callGraph.calledBy(m).size > 0 &&
                        // let's forget about System.arraycopy... causes too much
                        // noise/completely dominates the results
                        // E.g., typical use case: System.arraycopy(A,0,B,0,...)
                        !(m.name == "arraycopy" && classFile.thisType == ObjectType("java/lang/System"))
                }
            }
        println(
            calledNativeMethods.map(_.signatureToJava(false)).toList.sorted.
                mkString("Called Native Methods ("+calledNativeMethods.size+"):\n", "\n", "")
        )

        val mutex = new Object
        var results: List[NativeCallWithBoundedMethodParameter] = Nil
        def addResult(r: NativeCallWithBoundedMethodParameter): Unit = {
            mutex.synchronized { results = r :: results }
        }
        val unboundedCalls = new java.util.concurrent.atomic.AtomicInteger(0)

        for {
            nativeMethod ← calledNativeMethods //<= ParIterable
            // - The last argument to the method is the top-most stack value.
            // - For this analysis, we don't care whether we call a native instance
            //   or native static method.
            parametersCount = nativeMethod.parameterTypes.size
            parameterIndexes = nativeMethod.parameterTypes.zipWithIndex.collect {
                case (IntegerType /*| _: ReferenceType*/ , index) ⇒ index
            }
            // For the stack we don't distinguish between computational type
            // one and two category values; hence, no complex mapping is required.
            stackIndexes = parameterIndexes.map((parametersCount - 1) - _)
            (caller, callerPCs) ← callGraph.calledBy(nativeMethod)
            domain = new AnalysisDomain(theProject, caller)
            result = BaseAI(caller, domain)
        } {
            val pcs: org.opalj.collection.immutable.IntTrieSet = callerPCs //.iterable.toSeq
            for { pc ← pcs } {
                val operands = result.operandsArray(pc)
                if (operands != null)
                    for {
                        //<= this is practically the only place where a null check is necessary
                        stackIndex ← stackIndexes
                    } {
                        operands(stackIndex) match {
                            case result.domain.IntegerRange(lb, ub) ⇒
                                addResult(
                                    NativeCallWithBoundedMethodParameter(
                                        theProject,
                                        nativeMethod,
                                        parametersCount - stackIndex,
                                        caller,
                                        pc,
                                        lb,
                                        ub
                                    )
                                )
                            case _ ⇒
                                unboundedCalls.incrementAndGet()
                        }
                    }
            }
        }

        BasicReport(
            "Unbounded calls: "+unboundedCalls.get+"\n"+
                results.
                sortWith((l, r) ⇒
                    l.caller.classFile.thisType.id < r.caller.classFile.thisType.id).
                mkString("Bounded calls:\n", "\n\n", "\n")
        )

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

case class NativeCallWithBoundedMethodParameter(
        project:        SomeProject,
        nativeMethod:   Method,
        parameterIndex: Int,
        caller:         Method,
        callSite:       PC,
        lowerBound:     Int,
        upperBound:     Int
) {

    override def toString: String = {
        import Console._

        "The method "+
            BOLD + caller.toJava + RESET+
            " calls in line "+caller.body.get.lineNumber(callSite).getOrElse("N/A")+" the native method "+
            BOLD + BLUE + nativeMethod.toJava + RESET+
            " and passes in as the "+parameterIndex+
            ". parameter a bounded value: ["+lowerBound+","+upperBound+"]."
    }
}
