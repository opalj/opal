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
package ba

// the following is just a simple demo class which we are going to instrument
class SimpleInstrumentationDemo {

    def main(args: Array[String]): Unit = {
        new SimpleInstrumentationDemo().callsToString()
    }

    def callsToString(): Unit = {
        println("the length of the toString representation is: "+this.toString().length())
    }

    def returnsValue(i: Int): Int = {
        if (i % 2 == 0)
            return -1;
        else
            return 2;
    }

    def playingWithTypes(a: AnyRef): Unit = {
        a match {
            case i: Integer ⇒
                println("integer ")
                println(i.intValue())
            case c: java.util.Collection[_] ⇒
                print("some collection ")
                println(c) // let's assume that we want to know the type of c
            case s: String ⇒
                print("some string ")
                println(s)
        }
    }

    def endlessLoop(): Unit = {
        do {
            val t = System.currentTimeMillis
            val s = s"Juhu: $t"
            println(s)
        } while (System.currentTimeMillis % 200 < 10)
        println("Did it!")
    }

}
