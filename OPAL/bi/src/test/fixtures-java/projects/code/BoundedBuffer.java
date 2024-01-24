/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package code;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
public class BoundedBuffer {

    private final int buffer[];

    @SuppressWarnings("unused")
    private int first, last, size, numberInBuffer = 0;

    public BoundedBuffer(int length) {

        size = length;
        buffer = new int[size];
        first = last = 0;
    }

    public void put(int item) throws InterruptedException {
        // This is a deliberately wrong implementation!

        if (numberInBuffer == size)
            wait();

        last = (last + 1) % size; // % is modulus

        numberInBuffer++;

        buffer[last] = item;

        notifyAll();

    }

}