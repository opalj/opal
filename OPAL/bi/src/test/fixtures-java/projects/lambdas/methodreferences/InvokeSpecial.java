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

import java.util.function.Supplier;

import annotations.target.InvokedMethod;
import static annotations.target.TargetResolution.DYNAMIC;

/**
 * This class contains examples for method references which result in INVOKESPECIAL calls.
 *
 * <!--
 *
 *
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 *
 *
 * -->
 *
 * @author Andreas Muttscheller
 */
public class InvokeSpecial {

    public static class Superclass {
        private String interestingMethod() {
            return "Superclass";
        }

        @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/InvokeSpecial$Superclass", name = "$forward$interestingMethod", line = 58)
        public void exampleMethodTest() {
            Supplier<String> s = this::interestingMethod; // reference of a private method
            s.get();
        }

        protected String someMethod() {
            return "someMethod";
        }
    }

    public static class Subclass extends Superclass {

        String interestingMethod() {
            return "Subclass";
        }

        // name = "access$0", because of the inheritance of superclass. someMethod is accessed
        // via this access$0 method.
        @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/InvokeSpecial$Subclass", name = "access$0", line = 77)
        public String callSomeMethod() {
            Supplier<String> s = super::someMethod; // reference of a super method
            return s.get();
        }
    }

    public static void staticInheritanceWithParameter() {
        Subclass sc = new Subclass();
        sc.exampleMethodTest();
        sc.callSomeMethod();
    }
}
