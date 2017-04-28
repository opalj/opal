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
package cornercases;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
public interface MethodSignatures {

    void main(String[] args);

    void main(Object o, boolean b, double d);

    void main(double d, Object o);

    void main(Object o, boolean b, double d, Object o2);

    void main(boolean b, double d);

    void main(double d, float f, String[] args, long l);

    void main(boolean b, double d, float f, String[] args, long l, int i);

}

class MethodSignaturesCaller {

    MethodSignatures ms = null;

    void doIt1() {
        ms.main(new String[0]);
    }

    void doIt2() {
        ms.main(new Object(), true, 1.0d);
    }

    void doIt3() {
        ms.main(-1.0d, new Object());
    }

    void doIt4() {
        ms.main("Object o", false, 2.333d, "Object o2");
    }

    void doIt5() {
        ms.main(true, 33.33333d);
    }

    void doIt6() {
        ms.main(212.0d, 1.0f, new String[] { "a", "b" }, 12121l);
    }

}
