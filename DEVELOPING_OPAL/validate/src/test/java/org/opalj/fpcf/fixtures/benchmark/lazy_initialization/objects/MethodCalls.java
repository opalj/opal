/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.lazy_initialization.objects;

import org.opalj.fpcf.fixtures.benchmark.generals.Mutability;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;

public class MethodCalls {
    @ShallowImmutableField("")
    @LazyInitializedThreadSafeFieldReference("")
    private Mutability tm1;

    public synchronized void getTM1(){
        if(tm1==null){
            tm1= new Mutability();
        }
        tm1.nop();
    }

    @ShallowImmutableField("")
    @LazyInitializedThreadSafeFieldReference("")
    private Mutability tm2;

    public synchronized Mutability getTM2(){
        if(tm2==null){
            tm2= new Mutability();
        }
        return tm2;
    }

    @MutableField("")
    @LazyInitializedNotThreadSafeFieldReference("")
    private Mutability tm3;

    public void getTm3() {
        if(tm3==null){
            tm3 = new Mutability();
        }
    }

    @MutableFieldReference("")
    @MutableField("")
    private Mutability tm4;

    public synchronized Mutability getTm4() {
        if(tm4==null){
            tm4 = new Mutability();
        }
        return tm4;
    }

    public synchronized Mutability getTm42() {
        if(tm4==null){
            tm4 = new Mutability();
        }
        return tm4;
    }

    @ShallowImmutableField("")
    private Mutability tm5;

    public synchronized void getTm5() {
        if(tm5==null){
            tm5 = new Mutability();
        }
        tm5.nop();
    }

    @ShallowImmutableField("")
    @ImmutableFieldReference("")
    private Mutability tm6 = new Mutability();

    @ShallowImmutableField("")
    @ImmutableFieldReference("")
    private Mutability tm7 = new Mutability();

    public void foo(){
        tm7.nop();
    }

}


