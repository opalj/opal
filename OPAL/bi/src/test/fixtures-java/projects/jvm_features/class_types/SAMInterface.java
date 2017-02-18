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
package class_types;

import java.util.List;

@FunctionalInterface
interface GenericSAMInterface extends MarkerInterface, Cloneable {

    <T> boolean apply(List<T> o);

}

/**
 * Defines a functional interface/a single abstract method interface as used in the combination
 * with lambda expressions. A functional interface can define arbitrary constants and static
 * methods, but only one abstract instance method.
 *
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 *
 * @author Michael Eichberg
 */
@FunctionalInterface
@SuppressWarnings("all")
public interface SAMInterface extends GenericSAMInterface {

    String SIMPLE_STRING_CONSTANT = "CONSTANT";

    String COMPLEX_STRING_CONSTANT = new java.util.Date().toString();

    static boolean testHelper(Object o) {
        return o.getClass() == java.lang.Object.class;
    }

    boolean apply(List o);

    default void printDoIt(){System.out.println("Do it!");}

}

@FunctionalInterface
interface ExtSAMInterface extends SAMInterface {

        default void printExtended(){System.out.println("Extended!");}
}

// THIS IS NOT A SAMInterface anymore, though it it defines a single abstract method.
// @FunctionalInterface
interface SomeInterface extends SAMInterface {

    boolean call(Object o);

}

@SuppressWarnings("all")
class SAMInterfaceDemo {

 public SAMInterfaceDemo(SAMInterface i) {
     System.out.println(i.apply(null));
 }

 public static SAMInterfaceDemo factory() {
     SAMInterface i =  (List o) -> { return o == null; };
     return new SAMInterfaceDemo(i);
 }

 public static SAMInterfaceDemo factory(Object test) {
     return new SAMInterfaceDemo((List o) -> { return o == test; });
 }


}
