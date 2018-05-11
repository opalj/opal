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
package collection
package mutable

import java.util.Random
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Nanoseconds

/**
 * Evaluates the effectiveness of the locals data structure when compared with an array.
 */
object LocalsEval extends App {

    val REPETITIONS = 10000000
    var lastAvg = 0.0d
    val r = new Random

    val e = 1
    val eMax = 2
    val minRuns = 20
    println("Configuration")
    println("REPETITIONS = "+REPETITIONS)
    println("Timer Configuration")
    println("e = "+e)
    println("eMax = "+eMax)
    println("minRuns = "+minRuns)

    /////////

    def evalUsingLocals(elems: Int): Unit = {
        var lastAvg = 0L
        println(elems+" elments stored in vector")
        val data_v = time(e, eMax, minRuns, {
            var data: Locals[Integer] = Locals(elems)
            var i = 0
            while (i < REPETITIONS) {
                val index = r.nextInt(elems)
                val value = r.nextInt(10)
                val currentValue = data(index)
                data = data.updated(
                    index,
                    if (currentValue == null)
                        Integer.valueOf(value) else Integer.valueOf(currentValue + value)
                )
                i += 1
            }
            data
        }) { (t, ts) ⇒
            val sTs = ts.map(_.toSeconds).mkString(", ")
            val avg = ts.map(_.timeSpan).sum / ts.size
            if (lastAvg != avg) {
                lastAvg = avg
                val avgInSeconds = new Nanoseconds(lastAvg).toSeconds
                println(s"Avg: $avgInSeconds; T: ${t.toSeconds}; Ts: $sTs")
            }
        }

        println(data_v.mkString("Locals(", " : ", ")"))
    }

    def evalUsingArray(elems: Int): Unit = {
        var lastAvg = 0L
        println(elems+" elments stored in array")
        val data_a = time(e, eMax, minRuns, {
            var data = new Array[Integer](elems)
            var i = 0
            while (i < REPETITIONS) {
                val index = r.nextInt(elems)
                val value = r.nextInt(10)
                val newData = new Array[Integer](elems)
                System.arraycopy(data, 0, newData, 0, elems)
                val currentValue = data(index)
                newData(index) =
                    if (currentValue == null)
                        Integer.valueOf(value) else Integer.valueOf(currentValue + value)
                data = newData
                i += 1
            }
            data
        }) { (t, ts) ⇒
            val sTs = ts.mkString(", ")
            val avg = ts.map(_.timeSpan).sum / ts.size
            if (lastAvg != avg) {
                lastAvg = avg
                println(s"Avg: ${new Nanoseconds(avg).toSeconds}; T: $t; Ts: $sTs")
            }
        }
        println(data_a.mkString("Array(", " : ", ")"))
    }

    println(Console.BLUE); evalUsingLocals(1); println(Console.RESET)
    evalUsingArray(1); println(Console.RESET)

    println(Console.BLUE); evalUsingLocals(2); println(Console.RESET)
    evalUsingArray(2); println(Console.RESET)

    println(Console.BLUE); evalUsingLocals(3); println(Console.RESET)
    evalUsingArray(3); println(Console.RESET)

    println(Console.BLUE); evalUsingLocals(4); println(Console.RESET)
    evalUsingArray(4); println(Console.RESET)

    println(Console.BLUE); evalUsingLocals(5); println(Console.RESET)
    evalUsingArray(5); println(Console.RESET)

    println(Console.BLUE); evalUsingLocals(6); println(Console.RESET)
    evalUsingArray(6); println(Console.RESET)

    println(Console.BLUE); evalUsingLocals(7); println(Console.RESET)
    evalUsingArray(7); println(Console.RESET)

    println(Console.BLUE); evalUsingLocals(8); println(Console.RESET)
    evalUsingArray(8); println(Console.RESET)
}
