/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.lazy_initialization.objects.counterExamples;

//import edu.cmu.cs.glacier.qual.Immutable;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;

/**
 * This classes encompasses different counter examples of lazy initialization of field with object types.
 */
//@Immutable
public class Escaping {

    //@Immutable
    @AssignableField("The field is assigned to another field before the guarding if statement.")
    private Integer normalDoubleCheckedLockingEscapesBeforeGuard;

    //@Immutable
    @AssignableField("The field can be assigned multiple times.")
    private Integer escapingInteger;

    public synchronized Object getNormalDoubleCheckedLockingEscapesBeforeGuard() {
        this.escapingInteger = normalDoubleCheckedLockingEscapesBeforeGuard;
        if (this.normalDoubleCheckedLockingEscapesBeforeGuard == null)
            this.normalDoubleCheckedLockingEscapesBeforeGuard = new Integer(5);
        return this.normalDoubleCheckedLockingEscapesBeforeGuard;
    }

    //@Immutable
    @AssignableField("The field is read before it is assigned in the lazy initialization pattern.")
    private Integer doubleCheckedLockingAssignedToInstanceFieldEscapesWithinGuardBeforeAssignment;

        public synchronized Object getNormalDoubleCheckedLocking () {
        if (this.doubleCheckedLockingAssignedToInstanceFieldEscapesWithinGuardBeforeAssignment == null)
            this.escapingInteger = doubleCheckedLockingAssignedToInstanceFieldEscapesWithinGuardBeforeAssignment;
        this.doubleCheckedLockingAssignedToInstanceFieldEscapesWithinGuardBeforeAssignment = new Integer(5);
        return this.doubleCheckedLockingAssignedToInstanceFieldEscapesWithinGuardBeforeAssignment;
    }

    @AssignableField("The field value escapes via assignment to a static field before lazy initialization")
    private Integer doubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass;

    public synchronized Object getDoubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass() {
        StaticClass.integer = doubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass;
        if (this.doubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass == null)
            this.doubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass = new Integer(5);
        return this.doubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass;
    }

    @AssignableField("The field has two getters, that could interleave")
    private Integer integerWithTwoLazyInitializedGetters;

    public synchronized Integer getInteger1() {
        if(integerWithTwoLazyInitializedGetters ==null){
            integerWithTwoLazyInitializedGetters = new Integer(5);
        }
        return integerWithTwoLazyInitializedGetters;
    }

    public synchronized Integer getInteger2() {
        if(integerWithTwoLazyInitializedGetters ==null){
            integerWithTwoLazyInitializedGetters = new Integer(5);
        }
        return integerWithTwoLazyInitializedGetters;
    }
}

class StaticClass{
    public static Integer integer;
}

