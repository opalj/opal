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
package controlflow;

/**
 * Methods which contain definitive infinite loops (with complex internal contro-flow).
 * <p>
 * Primarily useful to test the computation of (post-)dominator trees/control-dependence graphs.
 *
 * @author Michael Eichberg
 */
public class InfiniteLoops {

    static void trivialInfiniteLoop(int i) {
        if (i < 0)
            return;
        else {
            while (true) {
                ;
            }
        }
    }

    static void regularLoopInInfiniteLoop(int i) {
        if (i < 0)
            return;
        else {
            while (true) {
                for (int j = 0; j < i; j++) {
                    j *= i;
                }
            }
        }
    }

    static void trivialNestedInfiniteLoops(int i) {
        if (i < 0)
            return;
        else {
            while (true) {
                if(i * 1 == i) {
                    while(true) {
                        ;
                    }
                }
            }
        }
    }

    static void nestedInfiniteLoops(int i) {
        if (i < 0)
            return;
        else {
            while (true) {
                if(i < 10){
                    while(true) {
                        i++;
                    }
                }
            }
        }
    }

    static void complexPathToInfiniteLoop(int i) {
        if (i < 0)
            return;
        else {
            if (i > 0) {
                i = i + 1;
            } else {
                i = -i;
            }
            i = i * i;
            while (true) {
                ;
            }
        }
    }

    static void infiniteLoopWithComplexPath(int i) {
        if (i < 0)
            return;
        else {
            for (; true; ) {
                if (i > 0) {
                    i = i + 1;
                } else {
                    i = -i;
                }
                i = i * i;
            }
        }
    }

    static void complexPathToInfiniteLoopWithComplexPath(int i) {
        if (i < 0)
            return;
        else {
            if (i > 0) {
                i = i + 1;
            } else {
                i = -i;
            }
            i = i * i;
            for (; true; ) {
                int j = i;
                if (j == 0) {
                    j = 1;
                } else {
                    j = -j + 2;
                }
                i = j * i;
            }
        }
    }

    static void multipleInfiniteLoops(int i) {
        if (i == -2332) while (true) { ; }

        if(i > 10000)
            return;

        if(i >= 0) {
            if(i > 100) {
                while (true) { // basic infinite loop
                    if(i+1 == 0) i--; // this "if" is always false....
                }
            } else if (i < 10 ){
                while (true) { // infinite loop with complex body
                    for (int j = 0 ; j < i; j++) { // regular loop in infinite loop
                        try {
                            while (true) { // only seemingly an infinite loop
                                System.out.println("test");
                            }
                        } catch (Throwable t) {
                            // let's forget about the exception
                        }
                    }
                }
            } else {
                while (true) {
                    i += 1;
                    if(i > 1000) {
                        do { // conditional nested infinite loop
                            i -= 1;
                        } while (true);
                    }
                }
            }
        } else {
            for (int j = -1111 ; j < i ; i++) { // regular loop
                System.out.println(j);
            }
        }
    }
}
