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
package calls.virtualCalls;

import base.AbstractBase;
import base.AlternateBase;
import base.ConcreteBase;
import base.SimpleBase;
import de.tud.cs.st.bat.test.invokedynamic.annotations.InvokedMethod;
import de.tud.cs.st.bat.test.invokedynamic.annotations.InvokedMethods;

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
public class CallConcreteObjects {

    // @InvokedConstructor(receiverType = SimpleBase.class, lineNumber = 12)
    SimpleBase simpleBase = new SimpleBase();

    // @InvokedConstructor(receiverType = ConcreteBase.class, lineNumber = 15)
    ConcreteBase concreteBase = new ConcreteBase();

    // @InvokedConstructor(receiverType = AlternateBase.class, lineNumber = 18)
    AlternateBase alternerateBase = new AlternateBase();

    // @InvokedConstructor(receiverType = AbstractBase.class, lineNumber = 21)
    AbstractBase abstractBase = new AbstractBase() {

        @Override
        public void abstractMethod() {
        }
    };

    @InvokedMethods({
            @InvokedMethod(receiverType = SimpleBase.class, name = "implementedMethod", lineNumber = 37),
            @InvokedMethod(receiverType = ConcreteBase.class, name = "implementedMethod", lineNumber = 38),
            @InvokedMethod(receiverType = AlternateBase.class, name = "implementedMethod", lineNumber = 39),
            @InvokedMethod(receiverType = AbstractBase.class, name = "implementedMethod", lineNumber = 40) })
    void callImplementedMethod() {
        simpleBase.implementedMethod();
        concreteBase.implementedMethod();
        alternerateBase.implementedMethod();
        abstractBase.implementedMethod();
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = SimpleBase.class, name = "abstractMethod", lineNumber = 50),
            @InvokedMethod(receiverType = ConcreteBase.class, name = "abstractMethod", lineNumber = 51),
            @InvokedMethod(receiverType = AlternateBase.class, name = "abstractMethod", lineNumber = 52),
            @InvokedMethod(receiverType = AbstractBase.class, name = "abstractMethod", lineNumber = 53) })
    void callAbstractMethod() {
        simpleBase.abstractMethod();
        concreteBase.abstractMethod();
        alternerateBase.abstractMethod();
        abstractBase.abstractMethod();
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = SimpleBase.class, name = "abstractImplementedMethod", lineNumber = 63),
            @InvokedMethod(receiverType = ConcreteBase.class, name = "abstractImplementedMethod", lineNumber = 64),
            @InvokedMethod(receiverType = AlternateBase.class, name = "abstractImplementedMethod", lineNumber = 65),
            @InvokedMethod(receiverType = AbstractBase.class, name = "abstractImplementedMethod", lineNumber = 66) })
    void callAbstractImplementedMethod() {
        simpleBase.abstractImplementedMethod();
        concreteBase.abstractImplementedMethod();
        alternerateBase.abstractImplementedMethod();
        abstractBase.abstractImplementedMethod();
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = SimpleBase.class, name = "interfaceMethod", lineNumber = 76),
            @InvokedMethod(receiverType = ConcreteBase.class, name = "interfaceMethod", lineNumber = 77),
            @InvokedMethod(receiverType = AlternateBase.class, name = "interfaceMethod", lineNumber = 78),
            @InvokedMethod(receiverType = AbstractBase.class, name = "interfaceMethod", lineNumber = 79) })
    void callInterfaceMethod() {
        simpleBase.interfaceMethod();
        concreteBase.interfaceMethod();
        alternerateBase.interfaceMethod();
        abstractBase.interfaceMethod();
    }
}
