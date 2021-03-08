/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@DeepImmutableClass("")
public class TriviallyEffectivelyNonAssignable {


    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private int n = 5;

    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private Object escapingViaConstructor = new Object();

    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private Object escapingViaGetter;


    public TriviallyEffectivelyNonAssignable(Object o) {
        this.escapingViaConstructor = o;
    }

    public Object getObject(){
        return escapingViaGetter;
    }
}
