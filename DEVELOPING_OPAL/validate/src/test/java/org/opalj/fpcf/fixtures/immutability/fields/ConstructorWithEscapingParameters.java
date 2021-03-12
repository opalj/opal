/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;

public class ConstructorWithEscapingParameters {

    @ShallowImmutableField("The field is init")
    @ImmutableFieldReference("The field is only assigned in the constructor.")
    private TrivialClass tc1;

   //TODO @DeepImmutableField("The construtor pararameter of the assigned object not escape")
    @ImmutableFieldReference("The field is only assigned in the constructor.")
    private TrivialClass tc2;

    ConstructorWithEscapingParameters(Object o1, Object o2){
        tc1 = new TrivialClass(o1, o2);
        tc2 = new TrivialClass(new Object(), new Object());
    }

}

class TrivialClass{
    private Object o1;
    private Object o2;
    public TrivialClass(Object o1, Object o2){
        this.o1 = o1;
        this.o2 = 02;
    }
}
