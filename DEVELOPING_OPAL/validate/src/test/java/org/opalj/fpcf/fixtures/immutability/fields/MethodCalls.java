/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;

public class MethodCalls {
    @ShallowImmutableField("")
    @LazyInitializedThreadSafeFieldReference("")
    private TestMutable tm1;

    public synchronized void getTM1(){
        if(tm1==null){
            tm1= new TestMutable();
        }
        tm1.nop();
    }

    @ShallowImmutableField("")
    @LazyInitializedThreadSafeFieldReference("")
    private TestMutable tm2;

    public synchronized TestMutable getTM2(){
        if(tm2==null){
            tm2= new TestMutable();
        }
        return tm2;
    }

    @MutableField("")
    @LazyInitializedNotThreadSafeFieldReference("")
    private TestMutable tm3;

    public void getTm3() {
        if(tm3==null){
            tm3 = new TestMutable();
        }
    }

    @MutableFieldReference("")
    @MutableField("")
    private TestMutable tm4;

    public synchronized TestMutable getTm4() {
        if(tm4==null){
            tm4 = new TestMutable();
        }
        return tm4;
    }

    public synchronized TestMutable getTm42() {
        if(tm4==null){
            tm4 = new TestMutable();
        }
        return tm4;
    }

    @ShallowImmutableField("")
    private TestMutable tm5;

    public synchronized void getTm5() {
        if(tm5==null){
            tm5 = new TestMutable();
        }
        tm5.nop();
    }

    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private TestMutable tm6 = new TestMutable();

    @ShallowImmutableField("")
    @ImmutableFieldReference("")
    private TestMutable tm7 = new TestMutable();

    public void foo(){
        tm7.nop();
    }








}

class TestMutable{
    private int n = 5;

    public void setN(int n){
        this.n = n;
    }

    public void nop(){
    }
}
