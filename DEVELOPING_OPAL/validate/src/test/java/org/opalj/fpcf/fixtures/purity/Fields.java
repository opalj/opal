/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.purity;

import org.opalj.br.fpcf.analyses.L0PurityAnalysis;
import org.opalj.fpcf.properties.purity.*;
import org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis;
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis;

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
    @Impure(value = "Allocates new objects/return value of super.clone not recognized as fresh",
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

    @DomainSpecificContextuallySideEffectFree(value = "The array the value is stored in is local",
            modifies = {0},
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

    @DomainSpecificContextuallySideEffectFree(value = "The array the value is stored in is local",
            modifies = {0},
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
