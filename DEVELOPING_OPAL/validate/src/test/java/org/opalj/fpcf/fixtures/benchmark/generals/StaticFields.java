/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generals;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("")
@DeepImmutableClass("")
public class StaticFields {

    @MutableFieldReference("")
    public static String name = "Class with static fields";

    @ImmutableFieldReference("")
    private static Object[] shallowArray = new Object[5];

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private static String deepImmutableString = "string";

    @MutableFieldReference("")
    private static int manualIncrementingCounter;

    @MutableFieldReference("")
    private static int manualCounter;

    @MutableFieldReference("")
    private static int instanceCounter;

    StaticFields() {
        instanceCounter = instanceCounter + 1;
    }

    public void incrementCounter() {
        manualIncrementingCounter = manualIncrementingCounter + 1;
    }

    public void setCounter(int n){
        manualCounter = n;
    }
}
