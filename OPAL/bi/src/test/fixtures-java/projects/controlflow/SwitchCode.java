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
 * Created to test the computation of control flow graphs.
 * 
 * @author Erich Wittenbeck
 */
public class SwitchCode {

    public int simpleSwitchWithBreakNoDefault(int a) {
        int res = 0;

        switch (a) {
        case 1:
            res = 123;
            break;
        case 2:
            res = 456;
            break;
        case 3:
            res = 789;
            break;
        }

        return res;
    }

    public int disparateSwitchWithoutBreakWithDefault(int a) {
        int res = 0;

        switch (a) {
        case 1:
            res++;
        case 42:
            res = res * 2;
        case 1337:
            res = res - 34;
        case Integer.MIN_VALUE:
            res = -1;
        default:
            res = 0;
        }

        return res;
    }

    public int withAndWithoutFallthrough(int a) {
        int res = 0;

        switch (a) {
        case 1:
            res = 12;
        case 2:
            res = 34;
        case 3:
            res = 56;
            break;
        case 4:
            res = 78;
            break;
        case 5:
            res = 910;
        default:
            res = 0;
        }

        return res;
    }

    public int degenerateSwitch(int a) {
        switch (a) {
        case 1:
            return 0;
        default:
            return 0;
        }
    }

}
