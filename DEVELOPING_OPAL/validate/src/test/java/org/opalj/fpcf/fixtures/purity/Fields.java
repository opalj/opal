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
package org.opalj.fpcf.fixtures.purity;

import org.opalj.fpcf.analyses.L0PurityAnalysis;
import org.opalj.fpcf.analyses.purity.L1PurityAnalysis;
import org.opalj.fpcf.analyses.purity.L2PurityAnalysis;
import org.opalj.fpcf.properties.purity.*;

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static org.opalj.fpcf.properties.purity.DomainSpecific.RaisesExceptions;
import static org.opalj.fpcf.properties.purity.DomainSpecific.UsesLogging;
import static org.opalj.fpcf.properties.purity.DomainSpecific.UsesSystemOutOrErr;

/**
 * Test cases for purity in the presence of local as well as non-local fields
 *
 * @author Dominik Helm
 */
public class Fields implements Cloneable {
    private int[] localPrivateField;
    int[] localPackagePrivateField;

    private int[] nonLocalPrivateField;

    @CompileTimePure("Initializes object")
    @Pure(value = "Initializes object", analyses = L1PurityAnalysis.class)
    @Impure(value = "Allocates new objects", analyses = L0PurityAnalysis.class)
    public Fields() {
        localPrivateField = new int[1];
        localPackagePrivateField = new int[2];
        nonLocalPrivateField = new int[3];
    }

    @DomainSpecificSideEffectFree(value = "Only creates copy (but can raise NullPointerException)",
            eps = @EP(cf = Object.class, pk = "ReturnValueFreshness",
                    method = "clone()Ljava/lang/Object;", p = "FreshReturnValue"))
    @Impure(value = "Allocates new objects",
            eps = @EP(cf = Object.class, pk = "ReturnValueFreshness",
                    method = "clone()Ljava/lang/Object;", p = "FreshReturnValue",
                    analyses = L2PurityAnalysis.class), negate = true)
    public Object clone() {
        Fields copy = null;
        try {
            copy = (Fields) super.clone();
        }catch(CloneNotSupportedException e){
            // Won't happen
        }
        copy.localPrivateField = new int[1];
        copy.localPackagePrivateField = new int[2];
        return copy;
    }

    @DomainSpecificExternallySideEffectFree(value = "The array the value is stored in is local",
    eps = @EP(cf = Fields.class, pk = "FieldLocality", field = "localPrivateField",
            p = "LocalField")
    )
    @Impure(value = "Analysis doesn't handle local fields/array not recognized as local",
            eps = @EP(cf = Fields.class, pk = "FieldLocality", field = "localPrivateField",
                    p = "LocalField", analyses = L2PurityAnalysis.class), negate = true
    )
    public void storeToLocalPrivateField(int value){
        localPrivateField[0] = value;
    }


    @DomainSpecificExternallySideEffectFree(value = "The array the value is stored in is local",
            eps = @EP(cf = Fields.class, pk = "FieldLocality", field = "localPackagePrivateField",
                    p = "LocalField")
    )
    @Impure(value = "Analysis doesn't handle local fields/array not recognized as local",
            eps = @EP(cf = Fields.class, pk = "FieldLocality", field = "localPackagePrivateField",
                    p = "LocalField", analyses = L2PurityAnalysis.class), negate = true
    )
    public void storeToLocalPackagePrivateField(int value){
        localPackagePrivateField[1] = value;
    }

    @Impure("The array the value is stored in is not local")
    public void storeToNonLocalPrivateField(int value){
        nonLocalPrivateField[2] = value;
    }
}
