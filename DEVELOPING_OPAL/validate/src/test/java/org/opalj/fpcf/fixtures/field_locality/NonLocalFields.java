/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.field_locality;

import org.opalj.fpcf.properties.field_locality.LocalFieldWithGetter;
import org.opalj.fpcf.properties.field_locality.NoLocalField;

public class NonLocalFields implements Cloneable {

    @NoLocalField("Not local as it is assigned by constructor")
    private Object assigned;

    @LocalFieldWithGetter("Not local as it escapes getValue")
    private Object escapes;

    @NoLocalField("Not local as cloned object may escape without this being overwritten")
    private Object copyEscapes;

    public NonLocalFields(Object input){
        assigned = input;
    }

    public Object getValue(){
        return escapes;
    }

    public Object clone() throws CloneNotSupportedException {
        NonLocalFields copy = (NonLocalFields) super.clone();
        copy.assigned = new Object();
        copy.escapes = new Object();
        if(System.nanoTime() > 1000){
            copy.copyEscapes = new Object();
        }else{
            copyEscapes = new Object(); // Does not assign field of the copy, but of the original!
        }
        return copy;
    }
}
