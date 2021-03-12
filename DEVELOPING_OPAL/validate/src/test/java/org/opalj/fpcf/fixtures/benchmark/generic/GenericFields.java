/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic;

import org.opalj.fpcf.fixtures.benchmark.generals.ClassWithMutableField;
import org.opalj.fpcf.properties.immutability.classes.ShallowImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("")
@ShallowImmutableClass("")
class GenericFields<T> {

    @DependentImmutableField("")
    private Generic<T> generic;

    @ShallowImmutableField("")
    private Generic<ClassWithMutableField> mutable = new Generic(new ClassWithMutableField());

    @DependentImmutableField("")
    private Generic<Generic<T>> nestedDependent;

    @ShallowImmutableField("")
    private Generic<Generic<ClassWithMutableField>> nestedShallow =
            new Generic<>(new Generic<>(new ClassWithMutableField()));

    public GenericFields(T t){
        this.generic = new Generic<>(t);
        this.nestedDependent = new Generic<>(new Generic<>(t));
    }

}

