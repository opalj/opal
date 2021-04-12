/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generals;

//import edu.cmu.cs.glacier.qual.Immutable;
import jdk.nashorn.internal.ir.annotations.Immutable;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("")
@MutableClass("")
public class Mutability {

    //@Immutable
    @MutableField("")
    @MutableFieldReference("")
    private Object o = new Object();

    public void setO(){
        this.o = o;
    }

    //@Immutable
    @MutableField("")
    @MutableFieldReference("The field can be incremented")
    private int iCompound;

    @Immutable
    @MutableField("")
    @MutableFieldReference("The field can be set")
    private int i;

    //@Immutable
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
            s.iCompound += 1;
        }
    }

    public static void setI(Mutability s, int n){
        if(s!=null){
            s.i = n;
        }
    }
}
