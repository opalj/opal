/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic.extended;

import org.opalj.fpcf.fixtures.benchmark.generals.ClassWithMutableField;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.DeepImmutableType;
import org.opalj.fpcf.properties.immutability.types.MutableType;
//import edu.cmu.cs.glacier.qual.Immutable;

class LowerUpperBounds<T extends ClassWithMutableField> {

    @ShallowImmutableField("")
    @ImmutableFieldReference("")
    private final T t;

    public LowerUpperBounds(T t){
        this.t = t;
    }
}


class C{

    //@Immutable
    @DeepImmutableField("only super type object")
    private final Generic<? super EmptyClass> g1;

    //@Immutable
    @ShallowImmutableField("")
    private final Generic<? extends EmptyClass> g2;

    //@Immutable
    @DeepImmutableField("")
    private final Generic<? extends X3> g3;

    //@Immutable
    @DeepImmutableField("")
    private final Generic<? super X3> g4;

    public C(Generic<? super EmptyClass> g1, Generic<? extends EmptyClass> g2, Generic<? extends X3> g3, Generic<? super X3> g4){
        this.g1 = g1;
        this.g2 = g2;
        this.g3 = g3;
        this.g4 = g4;
    }

    @MutableType("not final")
    @DeepImmutableClass("empty")
    class EmptyClass {}

    @MutableType("not final")
    @DeepImmutableClass("empty")
    class X2 extends EmptyClass {}

    @DeepImmutableType("final")
    @DeepImmutableClass("empty")
    final class X3 extends EmptyClass {}
}


