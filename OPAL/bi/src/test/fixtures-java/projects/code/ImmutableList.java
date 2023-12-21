/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package code;

import java.util.Iterator;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
public class ImmutableList<T> {

    private final T e;

    private final ImmutableList<T> next;

    /** Creates a new list with e as its single element. */
    public ImmutableList(T e) {

        this.e = e;
        next = null;
    }

    private ImmutableList(T e, ImmutableList<T> next) {

        this.e = e;
        this.next = next;
    }

    public T get() {

        return e;
    }

    public ImmutableList<T> getNext() {

        return next;
    }

    public ImmutableList<T> prepend(T e) {

        return new ImmutableList<T>(e, this);
    }

    @SuppressWarnings("synthetic-access")
    public Iterator<T> getIterator() {

        return new Iterator<T>() {

            ImmutableList<T> currentElement = ImmutableList.this;

            @Override
            public boolean hasNext() {

                return currentElement != null;
            }

            @Override
            public T next() {

                T e = currentElement.e;
                currentElement = currentElement.next;
                return e;
            }

            @Override
            public void remove() {

                throw new RuntimeException("not supported");
            }

        };
    }
}
