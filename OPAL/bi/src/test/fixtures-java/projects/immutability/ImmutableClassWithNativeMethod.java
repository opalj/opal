/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.annotations.Mutable;

/**
 * A class with a native method. A save approximation is to mark is as mutable.
 * 
 * @author Andre Pacak
 */
@Mutable("defines a native method")
public class ImmutableClassWithNativeMethod {

    private int x;

    public ImmutableClassWithNativeMethod(int x) {
        this.x = x;
    }

    public int getX() {
        return this.x;
    }

    public native void doSomething();
}
