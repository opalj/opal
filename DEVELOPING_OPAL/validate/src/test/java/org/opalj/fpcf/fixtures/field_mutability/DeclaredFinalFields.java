/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.field_mutability;

import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;

/**
 * Base class for tests below that calls a virtual method in its constructor that makes declared
 * final field visible in uninitialized state.
 *
 * @author Dominik Helm
 */
abstract class Super{
    public Super(){
        System.out.println(getD());
    }

    public abstract int getD();
}

/**
 * Tests for fields that are declared final. Some of them are not strictly final because they can
 * be observed uninitialized.
 *
 * @author  Dominik Helm
 */
public class DeclaredFinalFields extends Super {

    @ShallowImmutableField("Initialized directly")
    private final int a = 1;

    @ShallowImmutableField("Initialized through instance initializer")
    private final int b;

    @ShallowImmutableField("Initialized through constructor")
    private final int c;

    @MutableField(value = "Prematurely read through super constructor", prematurelyRead = true)
    private final int d;

    @MutableField(value = "Prematurely read through own constructor", prematurelyRead = true)
    private final int e;

    public DeclaredFinalFields() {
        super();
        c=1;
        d=1;
        System.out.println(getE());
        e=1;
    }

    public int getD(){
        return d;
    }

    public int getE(){
        return e;
    }

    // Instance initializer!
    {
        b = 1;
    }
}
