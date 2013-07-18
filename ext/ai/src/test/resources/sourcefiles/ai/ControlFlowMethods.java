/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
package ai;

/**
 * A very large number of methods that do not contain any control-flow statements.
 * 
 * <h2>NOTE</h2>
 * This class is not meant to be (automatically) recompiled; it just serves documentation 
 * purposes. The compiled class that is used by the tests is found in the test-classfiles 
 * directory.
 * 
 * @author Michael Eichberg
 */
public class ControlFlowMethods {

    //
    // COMPARISON
    
    public static boolean nullComp(Object o) {
        if (o == null)
            return true;
        else
            return false;
    }

    public static boolean nonnullComp(Object o) {
        if (o == null)
            return false;
        else
            return true;
    }


    // RELATIONAL OPERATORS
    // Comparing two values also invokes some kind of "IF" byte code operations which is 
    // a simple control flow.

    /*
     * FLOAD 0 FLOAD 1 FCMPL IFLE L1
     */
    public static boolean fCompFCMPL(float i, float j) {
        return i > j;
    }

    public static boolean fCompFCMPG(float i, float j) {
        return i < j;
    }

    public static boolean dCompDCMPL(double i, double j) {
        return i > j;
    }

    public static boolean dCompDCMPG(double i, double j) {
        return i < j;
    }

    public static boolean lCompDCMP(long i, long j) {
        return i == j;
    }

    public static boolean dCompDCMPG(int i, int j) {
        return i < j;
    }
}
