/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generic;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DependentImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@DependentImmutableClass("")
public class Generic<T> {

    //@Immutable
    @DependentImmutableField("")
    @ImmutableFieldReference("")
    T t;

    public Generic(T t){this.t = t;}
}
