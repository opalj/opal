/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.field_locality;

import org.opalj.fpcf.properties.field_locality.LocalField;
import org.opalj.fpcf.properties.field_locality.NoLocalField;

public class LocalFieldExample implements Cloneable {

    @LocalField("this field is local")
    private double[] data;

    @LocalField("another local field")
    private Object localField;

    public LocalFieldExample clone() throws CloneNotSupportedException {
        LocalFieldExample clone = (LocalFieldExample) super.clone();

        clone.data = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            clone.data[i] = data[i];
        }

        if(data.length > 5) {
            clone.localField = new Object();
        } else {
            clone.localField = new RuntimeException();
        }

        return clone;
    }

    public void add(LocalFieldExample other) {
        for (int i = 0; i < data.length; i++) {
            data[i] += other.data[i];
        }
    }

    public void resize(int size){
        data = new double[size];
    }
}
