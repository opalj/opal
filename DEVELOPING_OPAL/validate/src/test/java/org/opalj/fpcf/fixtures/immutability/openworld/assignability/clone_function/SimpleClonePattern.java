/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.assignability.clone_function;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;

/**
 * This class encompasses different possible cases of the clone pattern.
 */
@TransitivelyImmutableType("Class is final and transitively immutable")
@TransitivelyImmutableClass("Class has only transitively immutable fields")
public final class SimpleClonePattern {

    @TransitivelyImmutableField("Field is effectively non assignable and has a primitive type")
    @EffectivelyNonAssignableField("Field is only assigned ones due to the clone function pattern")
    private int i;

    public SimpleClonePattern clone(){
        SimpleClonePattern c = new SimpleClonePattern();
        c.i = i;
        return c;
    }
}

class CloneNonAssignableWithNewObject {

    @TransitivelyImmutableField("field is effectively non assignable and assigned with a transitively immutable object")
    @EffectivelyNonAssignableField("field is only assigned ones due to the clone function pattern")
    private Integer integer;

    public CloneNonAssignableWithNewObject clone(){
        CloneNonAssignableWithNewObject newInstance = new CloneNonAssignableWithNewObject();
        newInstance.integer = new Integer(5);
        return newInstance;
    }
}

class EscapesAfterAssignment {

    @TransitivelyImmutableField("field is effectively non assignable and assigned with a transitively immutable object")
    @EffectivelyNonAssignableField("field is only assigned ones due to the clone function pattern")
    private Integer integer;

    private Integer integerCopy;

    public EscapesAfterAssignment clone(){
        EscapesAfterAssignment newInstance = new EscapesAfterAssignment();
        newInstance.integer = new Integer(5);
        this.integerCopy = newInstance.integer;
        return newInstance;
    }
}

@TransitivelyImmutableType("Class is transitively immutable and final")
@TransitivelyImmutableClass("Class has only transitively immutable fields")
final class MultipleFieldsAssignedInCloneFunction {

    @TransitivelyImmutableField("The field is effectively non assignable and has a transitively immutable type")
    @EffectivelyNonAssignableField("The field is only assigned once in the clone function")
    private Integer firstInteger;

    @TransitivelyImmutableField("The field is effectively non assignable and has a transitively immutable type")
    @EffectivelyNonAssignableField("The field is only assigned once in the clone function")
    private Integer secondInteger;

    public MultipleFieldsAssignedInCloneFunction clone(){
        MultipleFieldsAssignedInCloneFunction newInstance = new MultipleFieldsAssignedInCloneFunction();
        newInstance.firstInteger = new Integer(5);
        newInstance.secondInteger = new Integer(5);
        return newInstance;
    }
}

class ConstructorWithParameter {

    @TransitivelyImmutableField("field is effectively non assignable and has a transitively immutable type")
    @EffectivelyNonAssignableField("field is only assigned ones due to the clone function pattern")
    private Integer integer;

    public ConstructorWithParameter(Integer integer){
        this.integer = integer;
    }
    public ConstructorWithParameter(){}

    public ConstructorWithParameter clone(Integer integer){
        ConstructorWithParameter newInstance = new ConstructorWithParameter(integer);
        newInstance.integer = this.integer;
        return newInstance;
    }
}

class CloneNonAssignableArrayWithRead {

    @EffectivelyNonAssignableField("field is only assigned once due to the clone function pattern")
    private boolean[] booleans;

    public CloneNonAssignableArrayWithRead clone(){
        CloneNonAssignableArrayWithRead newInstance = new CloneNonAssignableArrayWithRead();

        newInstance.booleans = new boolean[this.booleans.length];

        boolean dominated = newInstance.booleans[0];
        System.out.println(dominated);

        return newInstance;
    }
}

class CloneAssignableArrayWithNonDominatedRead {

    @AssignableField("field is assignable as one of the field reads is not dominated by a write")
    private boolean[] booleans;

    public CloneAssignableArrayWithNonDominatedRead clone(){
        CloneAssignableArrayWithNonDominatedRead newInstance = new CloneAssignableArrayWithNonDominatedRead();

        boolean nonDominated = newInstance.booleans[0];
        System.out.println(nonDominated);

        newInstance.booleans = new boolean[this.booleans.length];

        boolean dominated = newInstance.booleans[0];
        System.out.println(dominated);

        return newInstance;
    }
}
