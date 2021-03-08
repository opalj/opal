/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.fixtures.benchmark.generals.ClassWithMutableField;
import org.opalj.fpcf.fixtures.benchmark.generals.FinalEmptyClass;
import org.opalj.fpcf.properties.immutability.classes.ShallowImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("")
@ShallowImmutableClass("")
class GenericFields<T> {

    //@Immutable
    @DeepImmutableField("")
    private Generic<FinalEmptyClass> deepImmutable = new Generic<>(new FinalEmptyClass());

    @DependentImmutableField("")
    private Generic<T> generic;

    @ShallowImmutableField("")
    private Generic<ClassWithMutableField> mutable = new Generic(new ClassWithMutableField());


    @DependentImmutableField("")
    private Generic<Generic<T>> nestedDependent;

    @ShallowImmutableField("")
    private Generic<Generic<ClassWithMutableField>> nestedShallow =
            new Generic<>(new Generic<>(new ClassWithMutableField()));

    //@Immutable
    @DeepImmutableField("")
    private Generic<Generic<FinalEmptyClass>> nestedDeep =
            new Generic<>(new Generic<>(new FinalEmptyClass()));

    //@Immutable
    @DeepImmutableField("")
    Generic<org.opalj.fpcf.fixtures.immutability.fields.FinalEmptyClass> fecG =
            new Generic<>(new org.opalj.fpcf.fixtures.immutability.fields.FinalEmptyClass());

    public GenericFields(T t){
        this.generic = new Generic<>(t);
        this.nestedDependent = new Generic<>(new Generic<>(t));
    }

}




