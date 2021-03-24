/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generals;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@DeepImmutableClass("")
public class StaticFields {

    //@Immutable
    @MutableField("")
    @MutableFieldReference("")
    public static String name = "Class with static fields";

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private static String deepImmutableString = "string";

    //@Immutable
    @MutableField("")
    @MutableFieldReference("")
    private static int manualIncrementingCounter;

    //@Immutable
    @MutableField("")
    @MutableFieldReference("")
    private static int manualCounter;

    //@Immutable
    @MutableField("")
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
