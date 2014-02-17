/* License (BSD Style License):
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package FieldShouldBeFinal;

import java.util.Hashtable;

/**
 * @author Daniel Klauer
 */
public class VariousFields {

    /**
     * These fields are final already, and they are just int/String, so they are
     * definitely immutable, and should not be reported.
     */
    public static final int int1 = 123;
    public static final String s1 = "abc";

    /**
     * Both fields are non-final, and could be modified. They should be reported.
     */
    public static int int2 = 123;
    public static String s2 = "abc";

    /**
     * These fields are mutable despite being final, because they are not primitives. Only
     * the reference is final, not the array content, etc. The analysis does not report
     * these though.
     */
    public static final int[] array1 = new int[10];
    public static final Hashtable<String, Integer> hashtb1 = new Hashtable<String, Integer>();

    /**
     * These fields are non-final, but should not be reported because they are not
     * primitives. The analysis explicitly ignores arrays and hashtables to avoid
     * detecting too many false-positives.
     */
    public static int[] array2 = new int[10];
    public static Hashtable<String, Integer> hashtb2 = new Hashtable<String, Integer>();
}
