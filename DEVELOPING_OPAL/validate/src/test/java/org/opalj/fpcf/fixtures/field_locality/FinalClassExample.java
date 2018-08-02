/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.field_locality;

import org.opalj.fpcf.properties.field_locality.LocalField;
import org.opalj.fpcf.properties.field_locality.LocalFieldWithGetter;
import org.opalj.fpcf.properties.field_locality.NoLocalField;

public final class FinalClassExample {

    @LocalField("Local as class is final and not Cloneable")
    private int[] localData;

    @LocalFieldWithGetter("Not local, because it escapes in getNonLocalData")
    private int[] nonLocalData;

    public FinalClassExample(int size){
        localData = new int[size];
    }

    public int[] getNonLocalData(){
        return nonLocalData;
    }
}
