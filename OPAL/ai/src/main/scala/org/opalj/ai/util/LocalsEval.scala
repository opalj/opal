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
package util

import java.util.Random
import org.opalj.util.PerformanceEvaluation

object LocalsEval extends App {

    import org.opalj.util.PerformanceEvaluation._

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

    def evalUsingLocals(elems: Int) {
        var lastAvg = 0.0d
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
                        new Integer(value) else new Integer(currentValue + value))
                i += 1
            }
            data
        }) { (avg, t, ts) ⇒
            val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
            if (lastAvg != avg) {
                lastAvg = avg
                println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
            }
        }

        println(data_v.mkString("Locals(", " : ", ")"))
    }

    def evalUsingArray(elems: Int) {
        var lastAvg = 0.0d
        println(elems+" elments stored in array")
        val data_a = time(e, eMax, minRuns, {
            var data = new Array[Integer](elems)
            var i = 0
            while (i < REPETITIONS) {
                val index = r.nextInt(elems)
                val value = r.nextInt(10)
                var newData = new Array[Integer](elems)
                System.arraycopy(data, 0, newData, 0, elems)
                val currentValue = data(index)
                newData(index) =
                    if (currentValue == null)
                        new Integer(value) else new Integer(currentValue + value)
                data = newData
                i += 1
            }
            data
        }) { (avg, t, ts) ⇒
            val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
            if (lastAvg != avg) {
                lastAvg = avg
                println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
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

    /////////
    /*
    lastAvg = 0.0d
    println("5 elments stored in array - last three are updated")
    val data5_a_LAST4 = time(e, eMax, minRuns, {
        var data = Array[Integer](0, 0, 0, 0, 0)
        var i = 0
        while (i < REPETITIONS) {
            val index = r.nextInt(3) + 2
            val value = r.nextInt(10)
            var newData = new Array[Integer](5)
            System.arraycopy(data, 0, newData, 0, 5)
            val currentValue = data(index)
            newData(index) = new Integer(currentValue + value)
            data = newData
            i += 1
        }
        data
    }) { (avg, t, ts) ⇒
        val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
        if (lastAvg != avg) {
            lastAvg = avg
            println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
        }
    }
    println(data5_a_LAST4(0)+" : "+data5_a_LAST4(1)+" : "+data5_a_LAST4(2)+" : "+data5_a_LAST4(3)+" : "+data5_a_LAST4(4))

    /////////

    lastAvg = 0.0d
    println("5 elments stored in vector - last three are updated")
    val data5_v_LAST4 = time(e, eMax, minRuns, {
        var data: Locals[Integer] = Locals(IndexedSeq[Integer](0, 0, 0, 0, 0))
        var i = 0
        while (i < REPETITIONS) {
            val index = r.nextInt(3) + 2
            val value = r.nextInt(10)
            val currentValue = data(index)
            data = data.updated(
                index,
                if (currentValue == null)
                    new Integer(value) else new Integer(currentValue + value))
            i += 1
        }
        data
    }) { (avg, t, ts) ⇒
        val sTs = ts.map(t ⇒ f"${ns2sec(t)}%1.4f").mkString(", ")
        if (lastAvg != avg) {
            lastAvg = avg
            println(f"Avg: ${ns2sec(avg.toLong)}%1.4f; T: ${ns2sec(t)}%1.4f; Ts: $sTs")
        }
    }
    println(data5_v_LAST4(0)+" : "+data5_v_LAST4(1)+" : "+data5_v_LAST4(2)+" : "+data5_v_LAST4(3)+" : "+data5_v_LAST4(4))
*/
}