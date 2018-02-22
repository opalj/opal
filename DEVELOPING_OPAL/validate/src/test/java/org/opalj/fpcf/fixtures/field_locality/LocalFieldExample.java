package org.opalj.fpcf.fixtures.field_locality;

import org.opalj.fpcf.properties.field_locality.LocalField;

public class LocalFieldExample implements Cloneable {

    @LocalField("the field local")
    private double[] data;

    public LocalFieldExample clone() throws CloneNotSupportedException {
        LocalFieldExample clone = (LocalFieldExample) super.clone();

        clone.data = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            clone.data[i] = data[i];
        }

        return clone;
    }
}
