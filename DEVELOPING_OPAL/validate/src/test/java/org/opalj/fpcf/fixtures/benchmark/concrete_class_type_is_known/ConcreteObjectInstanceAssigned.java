/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.concrete_class_type_is_known;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@DeepImmutableClass("")
class ConcreteObjectInstanceAssigned {

    @DeepImmutableField(value = "concrete object is known")
    @ImmutableFieldReference("")
    private Object object = new Object();

    public Object getObject() {
        return this.object;
    }

}


