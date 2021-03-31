/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.allocation_freeness;

import org.opalj.fpcf.properties.allocation_freeness.AllocationFreeMethod;
import org.opalj.fpcf.properties.allocation_freeness.MethodWithAllocations;

/**
 * Tests for the AllocationFreeness property.
 *
 * @author Dominik Helm
 */
public class AllocationFreeness {

    private int field;

    @AllocationFreeMethod("Empty method")
    static void emptyMethod() { }

    @AllocationFreeMethod("Simple getter")
    private int getField() {
        return field;
    }

    @MethodWithAllocations("May throw null pointer exception")
    private int getField(AllocationFreeness other) {
        return other.field;
    }

    @AllocationFreeMethod("Simple setter")
    private void setField(int i){
        field = i;
    }

    @MethodWithAllocations("May throw null pointer exception")
    private void setField(AllocationFreeness other, int i){ other.field = i; }

    @AllocationFreeMethod("Calls method without allocations")
    private void allocationFreeCall(){
        emptyMethod();
    }

    @MethodWithAllocations("Direct allocation")
    private Object getNewObject(){
        return new Object();
    }

    @MethodWithAllocations("Calls method with allocation")
    private Object getNewObjectIndirect(){
        return getNewObject();
    }

    @MethodWithAllocations("Throws directly allocated exception")
    private void throwsExplicitException(){
        throw new RuntimeException();
    }

    @MethodWithAllocations("Throws implicit exception (division by zero)")
    private int divide(int divisor){
        return 10000/divisor;
    }
}
