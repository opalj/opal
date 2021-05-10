/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.sandbox60;

import org.opalj.fpcf.fixtures.benchmark.generals.ClassWithMutableFields;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;

//@MutableType("")
//@ShallowImmutableClass("")
class GenericFields<T> {

    @DependentImmutableField("")
    private Generic<T> generic;
    @NonTransitivelyImmutableField("")
    private Generic<ClassWithMutableFields> mutable = new Generic(new ClassWithMutableFields());

    @DependentImmutableField("")
    private Generic<Generic<T>> nestedDependent;

    @NonTransitivelyImmutableField("")
    private Generic<Generic<ClassWithMutableFields>> nestedShallow =
            new Generic<>(new Generic<>(new ClassWithMutableFields()));

    public GenericFields(T t){
        this.generic = new Generic<>(t);
        this.nestedDependent = new Generic<>(new Generic<>(t));
    }

}

