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
package lambdas.methodreferences;

import java.util.ArrayList;


/**
 * This class contains examples for method references dealing with proxy class receiver inheritance.
 *
 * <!--
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE.
 * -->
 *
 * @author Andreas Muttscheller
 */
public class SinkTest {
    // Copied from java.util.stream.Sink
    interface Sink<T> extends java.util.function.Consumer<T> {
        default void begin(long size) {}
        default void end() {}
        default boolean cancellationRequested() {
            return false;
        }
        default void accept(int value) {
            throw new IllegalStateException("called wrong accept method");
        }
        default void accept(long value) {
            throw new IllegalStateException("called wrong accept method");
        }
        default void accept(double value) {
            throw new IllegalStateException("called wrong accept method");
        }
        interface OfInt extends Sink<Integer> {
            @Override
            void accept(int value);

            @Override
            default void accept(Integer i) {
                accept(i.intValue());
            }
        }
    }

    public static class SinkOfInt implements Sink.OfInt {
        @Override
        public void accept(int value) {

        }
    }

    public static void downstreamTest() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        Sink<Integer> downstream = new SinkOfInt();
        downstream.begin(list.size());
        list.forEach(downstream::accept);
        downstream.end();
        list = null;
    }
}
