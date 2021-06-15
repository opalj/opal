/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.sandbox60;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@DependentlyImmutableClass("")
public class Generic<T> {

    @DependentlyImmutableField("")
    @EffectivelyNonAssignableField("")
    T t;
    public Generic(T t){this.t = t;}
}
