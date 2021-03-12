/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;

public class ArrayInitialization {

    @MutableFieldReference("")
    private Object[] array;

    public Object[] getArray(int n) {
        if (array == null || array.length < n) {
            this.array = new Object[n];
        }
        return array;
    }

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


class SimpleLazyObjectsInstantiation{

    @LazyInitializedNotThreadSafeFieldReference("")
    private SimpleLazyObjectsInstantiation instance;

    public SimpleLazyObjectsInstantiation getInstance() {
        if(instance==null)
            instance = new SimpleLazyObjectsInstantiation();
        return instance;
    }
}

class EscapingObjectDeep {
    //TODO
    @ShallowImmutableField(value = "", analyses = L3FieldImmutabilityAnalysis.class)
    private Object o;

    public synchronized Object getO(){
        if(this.o==null)
            this.o = new Object();
        return this.o;
    }
}

class EscapingObjectThatIsShallow {

    @ShallowImmutableField(value = "there are more than one object possibly assigned",
            analyses = L3FieldImmutabilityAnalysis.class)
    private final Object o;

    public EscapingObjectThatIsShallow() {
        this.o = new EmptyClass();
    }

    public EscapingObjectThatIsShallow(int n) {
        this.o = new Object();
    }

    public Object getO(){
        return this.o;
    }
}

class ClassUsingEmptyClass {

    //TODO   @DeepImmutableField(value = "concrete object is known", analyses = L3FieldImmutabilityAnalysis.class)
    private EmptyClass emptyClass = new EmptyClass();

    public EmptyClass getEmptyClass() {
        return this.emptyClass;
    }

}

class ClassUsingEmptyClassExtensible {

    @ShallowImmutableField(value = "all the concrete object that can be assigned are not known",
            analyses = L3FieldImmutabilityAnalysis.class)
    private EmptyClass emptyClass = new EmptyClass();

    public ClassUsingEmptyClassExtensible(EmptyClass emptyClass) {
        this.emptyClass = emptyClass;
    }
}

class EmptyClass {

}
