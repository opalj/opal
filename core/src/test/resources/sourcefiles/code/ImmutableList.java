/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package code;

import java.util.Iterator;

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

    @SuppressWarnings("hiding")
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

                @SuppressWarnings("hiding")
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
