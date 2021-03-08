/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generals;

import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("")
@MutableClass("")
public class Mutability {
    @MutableFieldReference("")
    private Object o = new Object();

    public void setO(){
        this.o = o;
    }

    @MutableField("")
    @MutableFieldReference("The field can be incremented")
    private int i;

    @MutableField("")
    @MutableFieldReference("")
    private int n = 5;

    public void setN(int n){
            this.n = n;
        }

        public void nop(){
        }

    public static void updateI(Mutability s) {
        if (s != null) {
            s.i += 1;
        }
    }
}
