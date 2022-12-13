/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.lazyinitialization.objects.counterExamples;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;

/**
 * This classes encompasses different counter examples of lazy initialization of field with object types.
 */
public class Escaping {

    @AssignableField("The field is assigned to another field before the guarding if statement.")
    private Object normalDoubleCheckedLockingEscapesBeforeGuard;

    @AssignableField("The field can be assigned multiple times.")
    private Object escapingObject;

    public synchronized Object getNormalDoubleCheckedLockingEscapesBeforeGuard() {
        this.escapingObject = normalDoubleCheckedLockingEscapesBeforeGuard;
        if (this.normalDoubleCheckedLockingEscapesBeforeGuard == null)
            this.normalDoubleCheckedLockingEscapesBeforeGuard = new Object();
        return this.normalDoubleCheckedLockingEscapesBeforeGuard;
    }

    @AssignableField("The field is read before it is assigned in the lazy initialization pattern.")
    private Object doubleCheckedLockingAssignedToInstanceFieldEscapesWithinGuardBeforeAssignment;

        public synchronized Object getNormalDoubleCheckedLocking () {
        if (this.doubleCheckedLockingAssignedToInstanceFieldEscapesWithinGuardBeforeAssignment == null) {
            this.escapingObject = doubleCheckedLockingAssignedToInstanceFieldEscapesWithinGuardBeforeAssignment;
            this.doubleCheckedLockingAssignedToInstanceFieldEscapesWithinGuardBeforeAssignment = new Object();
        }
        return this.doubleCheckedLockingAssignedToInstanceFieldEscapesWithinGuardBeforeAssignment;
    }

    @AssignableField("The field value escapes via assignment to a static field before lazy initialization")
    private Object doubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass;

    public synchronized Object getDoubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass() {
        StaticClass.object = doubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass;
        if (this.doubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass == null)
            this.doubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass = new Object();
        return this.doubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass;
    }

    @AssignableField("The field has two getters, that could interleave")
    private Object customObjectWithTwoLazyInitializedGetters;

    public synchronized Object getObject1() {
        if(customObjectWithTwoLazyInitializedGetters ==null){
            customObjectWithTwoLazyInitializedGetters = new Object();
        }
        return customObjectWithTwoLazyInitializedGetters;
    }

    public synchronized Object getObject2() {
        if(customObjectWithTwoLazyInitializedGetters ==null){
            customObjectWithTwoLazyInitializedGetters = new Object();
        }
        return customObjectWithTwoLazyInitializedGetters;
    }
}

class StaticClass{
    public static Object object;
}
