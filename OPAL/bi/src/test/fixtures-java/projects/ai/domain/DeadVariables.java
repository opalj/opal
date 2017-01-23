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
package ai.domain;

/**
 * This class contains various methods where some reference to values are dead at some
 * point in time.
 *
 * @author Michael Eichberg
 */
public class DeadVariables {

    static class ControlFlowException extends RuntimeException {

        static final long serialVersionUID = 0xcafebabe;
    };

    public void processIt(Object o) {
        /* EMPTY */}

    Object someFieldA = null;
    Object someFieldB = null;

    private int someInt = (int) (Math.random() * 100.0d);

    public Object deadOnTrueBranch(Object o) {
        Object v = o;
        if (v.hashCode() == 101212) {
            v = new Object(); // original v is dead...
            v.hashCode();
            return v;
        } else {
            return v;
        }
    }

    public Object deadOnFalseBranch(Object o) {
        Object v = o;
        if (v.hashCode() == 101212) {
            return v; // here, V ist NOT DEAD when we reach the return instruction!
        } else {
            v = new Object(); // original v is dead...
            v.hashCode();
            return v;
        }
    }

    public Object singleTargetAThrow() { // this is a variant of loopWithBreak
        ControlFlowException theCFE = new ControlFlowException();
        Object o = new Object();
        try {
            for (int i = 0; true; i++) {
                this.someFieldA = o;
                o = this.someFieldB;
                if (someInt < i) {
                    o = "useless"; // never used
                    throw theCFE; // this is "just a goto"...
                }
            }
        } catch (ControlFlowException cfe) {
            o = "kill it";
            return o;
        }
    }

    public Object loopWithBreak() { // this is a variant of singleTargetAThrow
        Object o = new Object();
        for (int i = 0; true; i++) {
            this.someFieldA = o;
            o = this.someFieldB;
            if (someInt < i) {
                o = "useless"; // never used
                break;
            }
        }
        o = "kill it";
        return o;

    }
}
