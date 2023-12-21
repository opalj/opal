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
package simpleCallgraph;

import org.opalj.ai.test.invokedynamic.annotations.CallGraphAlgorithm;
import org.opalj.ai.test.invokedynamic.annotations.InvokedConstructor;
import org.opalj.ai.test.invokedynamic.annotations.InvokedMethod;
import org.opalj.ai.test.invokedynamic.annotations.InvokedMethods;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is not meant to be (automatically) recompiled; it just serves documentation
 * purposes.
 * 
 * <!--
 * 
 * 
 * 
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * 
 * 
 * -->
 * 
 * @author Marco Jacobasch
 * @author Michael Reif
 */
public class A implements Base {

    Base b = new B();

    @Override
    @InvokedMethod(receiverType = "simpleCallgraph/B", name = "callOnInstanceField", line = 65)
    public String callOnInstanceField() {
        return b.callOnInstanceField();
    }

    @Override
    @InvokedMethod(receiverType = "simpleCallgraph/B", name = "callOnConstructor", line = 72)
    @InvokedConstructor(receiverType = "simpleCallgraph/B", line = 72)
    public void callOnConstructor() {
        new B().callOnConstructor();
    }

    @Override
    @InvokedMethods({
    	@InvokedMethod(receiverType = "simpleCallgraph/A", name = "callOnMethodParameter", line = 82),
            @InvokedMethod(receiverType = "simpleCallgraph/A", name = "callOnConstructor", line = 83),
            @InvokedMethod(receiverType = "simpleCallgraph/B", name = "callOnConstructor", line = 83)})
    public void callOnMethodParameter(Base b) {
        if (b != null) {
            this.callOnMethodParameter(null);
            b.callOnConstructor();
        }
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "simpleCallgraph/A", name = "callOnConstructor", line = 91),
            @InvokedMethod(receiverType = "simpleCallgraph/B", name = "callOnConstructor", line = 92)})
    public void directCallOnConstructor() {
        new A().callOnConstructor();
        new B().callOnConstructor();
    }

}
