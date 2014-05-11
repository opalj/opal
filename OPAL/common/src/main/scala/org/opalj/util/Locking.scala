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
package org.opalj
package util

/**
 * A basic facility to model shared and exclusive access to some functionality/data
 * structure.
 *
 * ==Usage==
 * To use this generic locking facility, you can either mix-in this trait or
 * create a new instance.
 *
 * @author Michael Eichberg
 */
trait Locking {

    import java.util.concurrent.locks.ReentrantReadWriteLock
    private[this] val rwLock = new ReentrantReadWriteLock()

    /**
     * Acquires the write lock associated with this instance and then executes
     * the function `f`. Afterwards, the lock is released.
     */
    def withWriteLock[B](f: ⇒ B): B = {
        try {
            rwLock.writeLock().lock()
            f
        } finally {
            rwLock.writeLock().unlock()
        }
    }

    /**
     * Acquires the read lock associated with this instance and then executes
     * the function `f`. Afterwards, the lock is released.
     */
    def withReadLock[B](f: ⇒ B): B = {
        try {
            rwLock.readLock().lock()
            f
        } finally {
            rwLock.readLock().unlock()
        }
    }
}
/**
 * Factory for `Locking` objects.
 */
object Locking {

    /**
     * Creates a new reentrant read-write lock.
     */
    def apply(): Locking = new Locking {}

}

