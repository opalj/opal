/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.ShallowImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.ShallowImmutableType;

//@Immutable
@ShallowImmutableType("")
@ShallowImmutableClass("")
public final class TriviallyEffectivelyNonAssignable {

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private int n = 5;

    //@Immutable
    @ShallowImmutableField("")
    @ImmutableFieldReference("")
    private Object escapingViaConstructor;

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private Object escapingViaGetter = new Object();

    public TriviallyEffectivelyNonAssignable(Object o) {
        this.escapingViaConstructor = o;
    }

    public Object getObject(){
        return escapingViaGetter;
    }
}
