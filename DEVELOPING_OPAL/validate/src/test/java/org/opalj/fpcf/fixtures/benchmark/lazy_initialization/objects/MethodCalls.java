/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.lazy_initialization.objects;

import org.opalj.fpcf.fixtures.benchmark.generals.ClassWithMutableFields;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.AssignableFieldReference;

public class MethodCalls {
    @NonTransitivelyImmutableField("")
    @LazyInitializedThreadSafeFieldReference("")
    private ClassWithMutableFields tm1;

    public synchronized void getTM1(){
        if(tm1==null){
            tm1= new ClassWithMutableFields();
        }
        tm1.nop();
    }

    @NonTransitivelyImmutableField("")
    @LazyInitializedThreadSafeFieldReference("")
    private ClassWithMutableFields tm2;

    public synchronized ClassWithMutableFields getTM2(){
        if(tm2==null){
            tm2= new ClassWithMutableFields();
        }
        return tm2;
    }

    @MutableField("")
    @LazyInitializedNotThreadSafeFieldReference("")
    private ClassWithMutableFields tm3;

    public void getTm3() {
        if(tm3==null){
            tm3 = new ClassWithMutableFields();
        }
    }

    @AssignableFieldReference("")
    @MutableField("")
    private ClassWithMutableFields tm4;

    public synchronized ClassWithMutableFields getTm4() {
        if(tm4==null){
            tm4 = new ClassWithMutableFields();
        }
        return tm4;
    }

    public synchronized ClassWithMutableFields getTm42() {
        if(tm4==null){
            tm4 = new ClassWithMutableFields();
        }
        return tm4;
    }

    @NonTransitivelyImmutableField("")
    private ClassWithMutableFields tm5;

    public synchronized void getTm5() {
        if(tm5==null){
            tm5 = new ClassWithMutableFields();
        }
        tm5.nop();
    }

    @NonTransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private ClassWithMutableFields tm6 = new ClassWithMutableFields();

    @NonTransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private ClassWithMutableFields tm7 = new ClassWithMutableFields();

    public void foo(){
        tm7.nop();
    }

}


