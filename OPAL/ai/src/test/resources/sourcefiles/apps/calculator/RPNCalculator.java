/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package apps.calculator;

import java.util.Stack;
import java.util.StringTokenizer;

public class RPNCalculator {

    // Things that we should be able to determine (at some point...)
    // 1.1) If expression is null, we will get a NullPointerException
    // 1.2) If expression is not null, we will not get a NullPointerException
    // 2) This method does not have any observable side effect (it is thread safe)
    // 3) The returned value is in the range Int.MinValue..Int.MaxValue
    // 4) We will never get an EmptyStackException

    @SuppressWarnings("boxing")
    public static int evaluate(String expression) {
        Stack<Integer> stack = new Stack<Integer>();

        StringTokenizer stringTokenizer = new StringTokenizer(expression);
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();

            if (token.matches("\\d+")) {
                stack.push(new Integer(token));
            } else {
                if (stack.size() < 2) {
                    throw new IllegalArgumentException("Missing operands for operator: " + token);
                }
                int r = stack.pop();
                int l = stack.pop();
                int t;
                if (token.equals("+")) {
                    t = l + r;
                } else if (token.equals("-")) {
                    t = l - r;
                } else if (token.equals("*")) {
                    t = l * r;
                } else if (token.equals("/")) {
                    t = l / r;
                } else {
                    throw new IllegalArgumentException("Unknown operator: " + token);
                }
                stack.push(t);
            }
        }
        if (stack.size() != 1) {
            throw new IllegalArgumentException("Too many operands.");
        }

        // the result is on top of the stack
        return stack.pop();
    }

    public static void main(String[] args) {
        String expression = "";
        for (String arg : args) {
            expression = expression + " " + arg;
        }
        System.out.println(evaluate(expression.trim()));
    }
}
