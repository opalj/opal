/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability.clone_function;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;

//@Immutable
@TransitivelyImmutableType("class ins final and transitively immutable")
@TransitivelyImmutableClass("class has only transitively immutable fields")
public final class SimpleClonePattern {

    //@Immutable
    @TransitivelyImmutableField("field is effectively non assignable and has a primitive type")
    @EffectivelyNonAssignableField("field is only assigned ones due to the clone function pattern")
    private int i;

    public SimpleClonePattern clone(){
        SimpleClonePattern c = new SimpleClonePattern();
        c.i = i;
        return c;
    }
}

class CloneNonAssignableWithNewObject {

    //@Immutable
    @TransitivelyImmutableField("field is effectively non assignable and assigned with a transitively immutable object")
    @EffectivelyNonAssignableField("field is only assigned ones due to the clone function pattern")
    private Object o;

    public CloneNonAssignableWithNewObject clone(){
        CloneNonAssignableWithNewObject c = new CloneNonAssignableWithNewObject();
        c.o = new Object();
        return c;
    }
}

class ConstructorWithParameter {

    //@Immutable
    @NonTransitivelyImmutableField("field is effectively non assignable but has not a transitively immutable type")
    @EffectivelyNonAssignableField("field is only assigned ones due to the clone function pattern")
    private Object o;

    public ConstructorWithParameter(Object o){
        this.o = o;
    }
    public ConstructorWithParameter(){}

    public ConstructorWithParameter clone(Object o){
        ConstructorWithParameter c = new ConstructorWithParameter(o);
        c.o = this.o;
        return c;
    }
}


