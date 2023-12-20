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
package callgraph.opal.algorithms;

import org.opalj.ai.test.invokedynamic.annotations.CallGraphAlgorithm;
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
 * <!-- INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * -->
 * 
 * @author Michael Reif
 */

public class Superclass {

    private SubclassLevel1 someSubtype;
    private Superclass top;

    public Superclass() {
        someSubtype = new SubclassLevel2();
        top = new SubclassLevel2();
    }

    public void implementedInEachSubclass() {
        this.toString();
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/Superclass", name = "implementedInEachSubclass", line = 92, isContainedIn = {
                    CallGraphAlgorithm.CHA, CallGraphAlgorithm.BasicVTA }),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/SubclassLevel1", name = "implementedInEachSubclass", line = 92, isContainedIn = {
                    CallGraphAlgorithm.CHA, CallGraphAlgorithm.BasicVTA }),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/SubclassLevel2", name = "implementedInEachSubclass", line = 92),

            @InvokedMethod(receiverType = "callgraph/opal/algorithms/Superclass", name = "implementedInEachSubclass", line = 93, isContainedIn = {}),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/SubclassLevel1", name = "implementedInEachSubclass", line = 93, isContainedIn = {
                    CallGraphAlgorithm.CHA, CallGraphAlgorithm.BasicVTA }),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/SubclassLevel2", name = "implementedInEachSubclass", line = 93),

            @InvokedMethod(receiverType = "callgraph/opal/algorithms/Superclass", name = "implementedInEachSubclass", line = 94, isContainedIn = {
                    CallGraphAlgorithm.CHA, CallGraphAlgorithm.BasicVTA }),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/SubclassLevel1", name = "implementedInEachSubclass", line = 94, isContainedIn = {
                    CallGraphAlgorithm.CHA, CallGraphAlgorithm.BasicVTA }),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/SubclassLevel2", name = "implementedInEachSubclass", line = 94), })
    public void callMethods() {
        getInstance().implementedInEachSubclass();
        someSubtype.implementedInEachSubclass();
        top.implementedInEachSubclass();
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/Superclass", name = "implementedInEachSubclass", line = 111, isContainedIn = { CallGraphAlgorithm.CHA }),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/SubclassLevel1", name = "implementedInEachSubclass", line = 111, isContainedIn = {
                    CallGraphAlgorithm.CHA, CallGraphAlgorithm.BasicVTA }),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/AltSubclassLevel2", name = "implementedInEachSubclass", line = 111, isContainedIn = {
                    CallGraphAlgorithm.CHA, CallGraphAlgorithm.BasicVTA,
                    CallGraphAlgorithm.DefaultVTA }),
            @InvokedMethod(receiverType = "callgraph/opal/algorithms/SubclassLevel2", name = "implementedInEachSubclass", line = 111) })
    public void testInstanceOfExtVTABranchElimination() {
        Superclass field = null;
        if (getInstance() instanceof AltSubclassLevel2)
            field = new AltSubclassLevel2();
        else
            field = this.someSubtype;
        field.implementedInEachSubclass();
    }

    private Superclass getInstance() {
        return top;
    }
}
