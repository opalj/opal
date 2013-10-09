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
package ai.domain;

/**
 * Methods that perform some operation and do some sanitization.
 * 
 * @author Michael Eichberg
 */
@SuppressWarnings("all")
public class Sanitization {

    static void sanitize(String s) { /* do nothing */
    }

    void notSanitized1(String s) {
        return;
    }

    void notSanitized2(String s) {
        if (System.nanoTime() > 0) {
            sanitize(s);
        }
    }

    void sanitized1(String s) {
        sanitize(s);
    }

    void sanitized2(String s) {
        if (System.nanoTime() > 0) {
            sanitize(s);
        } else {
            System.gc();
            sanitize(s);
        }
    }

    void sanitized3(String s) {
        if (s == null) {
            System.gc();
            sanitize(s);
            return;
        }

        if (s != null) {
            sanitize(s);
            return;
        }
    }

    void sanitized4(String s) {
        sanitize(s);
        if (s == null) {
            System.out.println("null");
            return;
        }

        if (s != null) {
            System.out.println(s);
            return;
        }
    }

    void sanitized5(String s) {
        if (s == null) {
            System.gc();
        }
        sanitize(s);
        if (s != null) {
            System.gc();
        }
    }
    
    void sanitized6(String s) {
        if (s == null) {
            System.gc();
        }
        sanitize(s);
        if (s != null) {
            System.gc();
            return;
        }
    }
    

    void sanitized7(String s) {
        if (s == null) {
            System.gc();
            sanitize(s);
        }

        if (s != null) {
            sanitize(s);
        }
    }
}
