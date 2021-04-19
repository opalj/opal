/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic.extended;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.fixtures.benchmark.generals.ClassWithMutableField;
import org.opalj.fpcf.properties.immutability.classes.ShallowImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@ShallowImmutableClass("")
class GenericFields<T> {

    //@Immutable
    @DependentImmutableField("")
    private final Generic<T> generic;

    //@Immutable
    @ShallowImmutableField("")
    private final Generic<ClassWithMutableField> mutable = new Generic(new ClassWithMutableField());

    //@Immutable
    @DependentImmutableField("")
    private final Generic<Generic<T>> nestedDependent;

    //@Immutable
    @ShallowImmutableField("")
    private final Generic<Generic<ClassWithMutableField>> nestedShallow =
            new Generic<>(new Generic<>(new ClassWithMutableField()));

    //@Immutable
    @DeepImmutableField("")
    private final Generic<Generic<FinalEmptyClass>> nestedDeep = new Generic<>(new Generic<>(new FinalEmptyClass()));

    //@Immutable
    @MutableField("")
    private Generic<Generic<T>> nestedMutable; // = new Generic<>(new Generic<>(new FinalEmptyClass()));

    public void setNestedMutable(Generic<Generic<T>> nestedMutable){
        this.nestedMutable = nestedMutable;
    }

    public GenericFields(T t){
        this.generic = new Generic<>(t);
        this.nestedDependent = new Generic<>(new Generic<>(t));
    }
    final class FinalEmptyClass{}

}

