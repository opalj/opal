/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.lazy_initialization.objects.counterExamples;

//import edu.cmu.cs.glacier.qual.Immutable;

import org.opalj.fpcf.fixtures.benchmark.commons.CustomObject;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;

/**
 * This classes encompasses different counter examples of lazy initialization of field with object types.
 */
//@Immutable
public class Escaping {

    //@Immutable
    @AssignableField("The field is assigned to another field before the guarding if statement.")
    private CustomObject normalDoubleCheckedLockingEscapesBeforeGuard;

    //@Immutable
    @AssignableField("The field can be assigned multiple times.")
    private CustomObject escapingInteger;

    public synchronized CustomObject getNormalDoubleCheckedLockingEscapesBeforeGuard() {
        this.escapingInteger = normalDoubleCheckedLockingEscapesBeforeGuard;
        if (this.normalDoubleCheckedLockingEscapesBeforeGuard == null)
            this.normalDoubleCheckedLockingEscapesBeforeGuard = new CustomObject();
        return this.normalDoubleCheckedLockingEscapesBeforeGuard;
    }

    //@Immutable
    @AssignableField("The field is read before it is assigned in the lazy initialization pattern.")
    private CustomObject doubleCheckedLockingAssignedToInstanceFieldEscapesWithinGuardBeforeAssignment;

        public synchronized CustomObject getNormalDoubleCheckedLocking () {
        if (this.doubleCheckedLockingAssignedToInstanceFieldEscapesWithinGuardBeforeAssignment == null) {
            this.escapingInteger = doubleCheckedLockingAssignedToInstanceFieldEscapesWithinGuardBeforeAssignment;
            this.doubleCheckedLockingAssignedToInstanceFieldEscapesWithinGuardBeforeAssignment = new CustomObject();
        }
        return this.doubleCheckedLockingAssignedToInstanceFieldEscapesWithinGuardBeforeAssignment;
    }

    @AssignableField("The field value escapes via assignment to a static field before lazy initialization")
    private CustomObject doubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass;

    public synchronized CustomObject getDoubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass() {
        StaticClass.customObject = doubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass;
        if (this.doubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass == null)
            this.doubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass = new CustomObject();
        return this.doubleCheckedLockingAssignedToPublicStaticFieldOfAnotherClass;
    }

    @AssignableField("The field has two getters, that could interleave")
    private CustomObject customObjectWithTwoLazyInitializedGetters;

    public synchronized CustomObject getCustomObject1() {
        if(customObjectWithTwoLazyInitializedGetters ==null){
            customObjectWithTwoLazyInitializedGetters = new CustomObject();
        }
        return customObjectWithTwoLazyInitializedGetters;
    }

    public synchronized CustomObject getCustomObject2() {
        if(customObjectWithTwoLazyInitializedGetters ==null){
            customObjectWithTwoLazyInitializedGetters = new CustomObject();
        }
        return customObjectWithTwoLazyInitializedGetters;
    }
}

class StaticClass{
    public static CustomObject customObject;
}

