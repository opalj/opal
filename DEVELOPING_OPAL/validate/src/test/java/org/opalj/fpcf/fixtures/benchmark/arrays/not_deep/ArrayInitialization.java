/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.arrays.not_deep;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@MutableClass("")
public class ArrayInitialization {

    //@Immutable
    @MutableField("")
    @MutableFieldReference("")
    private Object[] array;

    public Object[] getArray(int n) {
        if (array == null || array.length < n) {
            this.array = new Object[n];
        }
        return array;
    }

    //@Immutable
    @MutableField("")
    @MutableFieldReference("")
    private Object[] b;

    public Object[] getB(boolean flag) throws Exception {
        if(b!=null)
            return b;
        else if(flag)
            return b; //throw new Exception("");
        else {
            this.b = new Object[5];
            return b;
        }
    }
}







