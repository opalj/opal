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
package org.opalj.fpcf.fixtures.purity;

import org.opalj.fpcf.analyses.L0PurityAnalysis;
import org.opalj.fpcf.analyses.L2PurityAnalysis;
import org.opalj.fpcf.analyses.L1PurityAnalysis;
import org.opalj.fpcf.properties.purity.*;

/**
 * Test cases for purity in the presence of virtual calls
 *
 * @author Dominik Helm
 */
public class VirtualCalls {

    public interface AnInterface {

        // This method has pure (SubClassA) and impure (SubClassB) implementations
        int interfaceMethod(int i);
    }

    public abstract class BaseClass {

        // This method has pure (SubClassA) and side-effect free (SubClassB) implementations
        public abstract int abstractMethod(int i);

        // This (pure) method has an impure override in SubClassB
        @CompileTimePure("Only returns immutable parameter")
        @Pure(value = "Only returns immutable parameter",
                analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
        public int nonAbstractMethod(int i) {
            return i;
        }

        @CompileTimePure("Only returns double of immutable parameter")
        @Pure(value = "Only returns double of immutable parameter",
                analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
        public final int finalMethod(int i) {
            return i * 2;
        }
    }

    public class SubClassA extends BaseClass implements AnInterface {

        @CompileTimePure("Only returns result of exception-free computation on immutable parameter")
        @Pure(value = "Only returns result of exception-free computation on immutable parameter",
                analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
        public int interfaceMethod(int i) {
            return i * 2;
        }

        @CompileTimePure("Only returns result of exception-free computation on immutable parameter")
        @Pure(value = "Only returns result of cexception-free omputation on immutable parameter",
                analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
        public final int abstractMethod(int i) {
            return i + 2;
        }

        @CompileTimePure("Only returns result of exception-free computation on immutable parameter")
        @Pure(value = "Only returns result of cexception-free omputation on immutable parameter",
                analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
        public int nonAbstractMethod(int i) {
            if (i > 0)
                return i;
            else
                return -1;
        }
    }

    public class SubClassB extends BaseClass implements AnInterface {

        private int nonFinal = 5;

        @Impure("Uses native method System.nanoTime")
        public int interfaceMethod(int i) {
            return (int) (i + System.getenv().size());
        }

        @SideEffectFree("Uses value of instance field")
        @Impure(value = "Uses instance field", analyses = L0PurityAnalysis.class)
        public final int abstractMethod(int i) {
            return i + nonFinal;
        }

        @ExternallySideEffectFree("modifies and returns instance field nonFinal")
        @Impure(value = "modifies instance field nonFinal",
                analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
        public int nonAbstractMethod(int i) {
            nonFinal += i;
            return nonFinal;
        }
    }

    public final class SubClassC extends BaseClass {

        @CompileTimePure("returns constant 0")
        @Pure(value = "returns constant 0",
                analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
        public int abstractMethod(int i) {
            return 0;
        }
    }

    @Impure("interfaceMethod could be implemented in a subclass of SubClassA that is not available")
    public int impureInterfaceCall1(SubClassA a) {
        AnInterface ai = a;
        if (ai == null)
            return 0;
        return ai.interfaceMethod(5);
    }

    @Impure("interfaceMethod could be implemented in an unknown implementor of AnInterface")
    public int impureInterfaceCall2(AnInterface ai) {
        if (ai == null)
            return 0;
        return ai.interfaceMethod(5);
    }

    @Pure(value = "Calls pure method on object with precise type",
            analyses = { L1PurityAnalysis.class, L2PurityAnalysis.class })
    @Impure(value = "Analysis doesn't handle virtual calls", analyses = L0PurityAnalysis.class)
    public int pureInterfaceCall(int i) {
        AnInterface ai = new SubClassA();
        if (ai == null)
            return 0;
        return ai.interfaceMethod(i);
    }

    @Pure(value = "Calls pure final method",
            analyses = { L1PurityAnalysis.class, L2PurityAnalysis.class })
    @Impure(value = "Analysis doesn't handle virtual calls", analyses = L0PurityAnalysis.class)
    public int abstractMethodCall1(SubClassA a) {
        BaseClass bc = a;
        if (bc == null)
            return 0;
        return bc.abstractMethod(5);
    }

    @SideEffectFree("Calls side-effect free final method")
    @Impure(value = "Calls impure final method", analyses = L0PurityAnalysis.class)
    public int abstractMethodCall2(SubClassB b) {
        BaseClass bc = b;
        if (bc == null)
            return 0;
        return bc.abstractMethod(5);
    }

    @Pure(value = "Calls pure method of final class",
            analyses = { L1PurityAnalysis.class, L2PurityAnalysis.class })
    @Impure(value = "Analysis doesn't handle virtual calls", analyses = L0PurityAnalysis.class)
    public int abstractMethodCall3(SubClassC c) {
        BaseClass bc = c;
        if (bc == null)
            return 0;
        return bc.abstractMethod(5);
    }

    @Impure("abstractMethod could be overriden in a subtype of BaseClass that is not available")
    public int abstractMethodCall4(BaseClass bc) {
        if (bc == null)
            return 0;
        return bc.abstractMethod(5);
    }

    @Pure(value = "Calls pure method on object with precise type",
            analyses = { L1PurityAnalysis.class, L2PurityAnalysis.class })
    @Impure(value = "Analysis doesn't handle virtual calls", analyses = L0PurityAnalysis.class)
    public int pureAbstractMethodCall(int i) {
        BaseClass bc = new SubClassA();
        return bc.abstractMethod(i);
    }

    @SideEffectFree(value = "Calls side-effect free method on object with precise type")
    @Impure(value = "Analysis doesn't handle virtual calls", analyses = L0PurityAnalysis.class)
    public int sideEffectFreeAbstractMethodCall(int i) {
        BaseClass bc = new SubClassB();
        return bc.abstractMethod(i);
    }

    @Impure("nonAbstractMethod could be overriden in a subtype of BaseClass that is not available")
    public int impureNonAbstractMethodCall(BaseClass bc) {
        if (bc == null)
            return 0;
        return bc.nonAbstractMethod(5);
    }

    @Pure(value = "Calls pure method on object with precise type",
            analyses = { L1PurityAnalysis.class, L2PurityAnalysis.class })
    @Impure(value = "Analysis doesn't handle virtual calls", analyses = L0PurityAnalysis.class)
    public int pureNonAbstractMethodCall(int i) {
        BaseClass bc = new SubClassA();
        return bc.nonAbstractMethod(i);
    }

    @Pure(value = "Calls final pure method",
            analyses = { L1PurityAnalysis.class, L2PurityAnalysis.class })
    @Impure(value = "Analysis doesn't handle virtual calls", analyses = L0PurityAnalysis.class)
    public int finalCall1(BaseClass bc, int i) {
        if (bc == null)
            return 0;
        return bc.finalMethod(i);
    }

    @Pure(value = "Calls final pure method",
            analyses = { L1PurityAnalysis.class, L2PurityAnalysis.class })
    @Impure(value = "Analysis doesn't handle virtual calls", analyses = L0PurityAnalysis.class)
    public int finalCall2(SubClassA a, int i) {
        if (a == null)
            return 0;
        return a.finalMethod(i);
    }

    @Pure(value = "Calls final pure method",
            analyses = { L1PurityAnalysis.class, L2PurityAnalysis.class })
    @Impure(value = "Analysis doesn't handle virtual calls", analyses = L0PurityAnalysis.class)
    public int finalCall3(SubClassC c, int i) {
        if (c == null)
            return 0;
        return c.finalMethod(i);
    }
}
