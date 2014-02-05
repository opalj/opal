/* License (BSD Style License):
 * Copyright (c) 2009, 2011
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Software Technology Group or Technische
 *   Universität Darmstadt nor the names of its contributors may be used to
 *   endorse or promote products derived from this software without specific
 *   prior written permission.
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
package calls.misc;

import base.Base;
import base.SimpleBase;
import de.tud.cs.st.bat.test.invokedynamic.annotations.InvokedConstructor;
import de.tud.cs.st.bat.test.invokedynamic.annotations.InvokedMethod;

/**
 * This class was used to create a class file with some well defined attributes. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is not meant to be (automatically) recompiled; it just serves documentation
 * purposes.
 * 
 * @author Marco Jacobasch
 */
public class TryCatchFinally {

    @InvokedConstructor(receiverType = SimpleBase.class, lineNumber = 16)
    @InvokedMethod(receiverType = SimpleBase.class, name = "implementedMethod", lineNumber = 17)
    void callVirtualMethodInCatch() {
        try {
            throw new Exception();
        } catch (Exception e) {
            Base base = new SimpleBase();
            base.implementedMethod();
        }
    }

    @InvokedMethod(receiverType = SimpleBase.class, name = "staticMethod", isStatic = true, lineNumber = 26)
    void callStaticMethodInCatch() {
        try {
            throw new Exception();
        } catch (Exception e) {
            SimpleBase.staticMethod();
        }
    }

    @InvokedConstructor(receiverType = SimpleBase.class, lineNumber = 37)
    @InvokedMethod(receiverType = SimpleBase.class, name = "implementedMethod", lineNumber = 38)
    void notCallVirtualMethodInCatch() {
        try {
            @SuppressWarnings("unused")
            int i = 0;
        } catch (Exception e) {
            Base base = new SimpleBase();
            base.implementedMethod();
        }
    }

    @InvokedMethod(receiverType = SimpleBase.class, name = "staticMethod", isStatic = true, lineNumber = 48)
    void notCallStaticMethodInCatch() {
        try {
            @SuppressWarnings("unused")
            int i = 0;
        } catch (Exception e) {
            SimpleBase.staticMethod();
        }
    }

    @InvokedConstructor(receiverType = SimpleBase.class, lineNumber = 60)
    @InvokedMethod(receiverType = SimpleBase.class, name = "implementedMethod", lineNumber = 61)
    void callVirtualMethodInFinally() {
        try {
            throw new Exception();
        } catch (Exception e) {
        } finally {
            Base base = new SimpleBase();
            base.implementedMethod();
        }
    }

    @InvokedMethod(receiverType = SimpleBase.class, name = "staticMethod", isStatic = true, lineNumber = 71)
    void callStaticMethodInFinally() {
        try {
            throw new Exception();
        } catch (Exception e) {
        } finally {
            SimpleBase.staticMethod();
        }
    }

    // TODO more precise catch
}
