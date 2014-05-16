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
package org.opalj.br

import org.opalj.util.PerformanceEvaluation
import org.opalj.util.PerformanceEvaluation.{ time, ns2sec }

object ObjectTypes {

    val theType1 = ObjectType("java/lang/Boolean")
    val theType2 = ObjectType("java/lang/Integer")
    val theType3 = ObjectType("java/lang/Double")
    val alternateType = ObjectType("FooBar")

    val e1 = new PerformanceEvaluation()

    time(1, 2, 5, {
        var result = 0
        var i = 0
        while (i < 1000000) {
            val (testType, time) = e1.time('setup) {
                (
                    {
                        val rndValue = java.lang.Math.random()
                        if (rndValue < 0.99d) alternateType
                        else if (rndValue < 0.993333d) theType1
                        else if (rndValue < 0.996666d) theType2
                        else theType3
                    },
                    System.nanoTime
                )
            }
            e1.time('match) {
                result += (
                    testType match {
                        case ObjectType.Boolean ⇒ (time % 10000).toInt
                        case ObjectType.Byte    ⇒ (time % 20000).toInt
                        case ObjectType.Char    ⇒ (time % 30000).toInt
                        case ObjectType.Short   ⇒ (time % 40000).toInt
                        case ObjectType.Integer ⇒ (time % 50000).toInt
                        case ObjectType.Long    ⇒ (time % 60000).toInt
                        case ObjectType.Float   ⇒ (time % 70000).toInt
                        case ObjectType.Double  ⇒ (time % 80000).toInt

                        case _                          ⇒ 0
                    }
                )
            }
            i += 1
        }
        "After "+i+" runs the result is :"+result
    }) { (avg, t, ts) ⇒
        val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
        println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
    }
    println(e1.getTime('setup))
    println(e1.getTime('match))

    val e2 = new PerformanceEvaluation()

    val matcher = ObjectType.primitiveWrapperMatcher[(Long), Int](
        (v: Long) ⇒ (v % 10000).toInt,
        (v: Long) ⇒ (v % 20000).toInt,
        (v: Long) ⇒ (v % 30000).toInt,
        (v: Long) ⇒ (v % 40000).toInt,
        (v: Long) ⇒ (v % 50000).toInt,
        (v: Long) ⇒ (v % 60000).toInt,
        (v: Long) ⇒ (v % 70000).toInt,
        (v: Long) ⇒ (v % 80000).toInt,
        (v: Long) ⇒ 0)
    time(1, 2, 5, {

        var result = 0
        var i = 0
        while (i < 1000000) {
            val (testType, time) = e2.time('setup) {
                (
                    {
                        val rndValue = java.lang.Math.random()
                        if (rndValue < 0.99d) alternateType
                        else if (rndValue < 0.993333d) theType1
                        else if (rndValue < 0.996666d) theType2
                        else theType3
                    },
                    System.nanoTime
                )
            }
            e2.time('match) {
                result += matcher(testType, time)
            }
            i += 1
        }
        "After "+i+"runs the result is :"+result
    }) { (avg, t, ts) ⇒
        val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
        println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
    }

    println(e2.getTime('setup))
    println(e2.getTime('match))
}