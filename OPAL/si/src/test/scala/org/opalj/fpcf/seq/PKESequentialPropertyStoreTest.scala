/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package org.opalj.fpcf
package seq

class TrueTrueTruePKESequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore()
        s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
            delayHandlingOfFinalDependeeUpdates = true,
            delayHandlingOfNonFinalDependeeUpdates = true
        )
        s.delayHandlingOfDependerNotification = true
        s.suppressError = true
        s
    }

}

class FalseTrueTruePKESequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore()
        s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
            delayHandlingOfFinalDependeeUpdates = false,
            delayHandlingOfNonFinalDependeeUpdates = true
        )
        s.delayHandlingOfDependerNotification = true
        s.suppressError = true
        s
    }
}

class TrueFalseTruePKESequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore()
        s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
            delayHandlingOfFinalDependeeUpdates = true,
            delayHandlingOfNonFinalDependeeUpdates = false
        )
        s.delayHandlingOfDependerNotification = true
        s.suppressError = true
        s
    }
}

class TrueTrueFalsePKESequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore()
        s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
            delayHandlingOfFinalDependeeUpdates = true,
            delayHandlingOfNonFinalDependeeUpdates = true
        )
        s.delayHandlingOfDependerNotification = false
        s.suppressError = true
        s
    }
}

class FalseFalseTruePKESequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore()
        s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
            delayHandlingOfFinalDependeeUpdates = false,
            delayHandlingOfNonFinalDependeeUpdates = false
        )
        s.delayHandlingOfDependerNotification = true
        s.suppressError = true
        s
    }
}

class FalseTrueFalsePKESequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore()
        s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
            delayHandlingOfFinalDependeeUpdates = false,
            delayHandlingOfNonFinalDependeeUpdates = true
        )
        s.delayHandlingOfDependerNotification = false
        s.suppressError = true
        s
    }
}

class TrueFalseFalsePKESequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore()
        s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
            delayHandlingOfFinalDependeeUpdates = true,
            delayHandlingOfNonFinalDependeeUpdates = false
        )
        s.delayHandlingOfDependerNotification = false
        s.suppressError = true
        s
    }
}

class FalseFalseFalsePKESequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore()
        s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
            delayHandlingOfFinalDependeeUpdates = false,
            delayHandlingOfNonFinalDependeeUpdates = false
        )
        s.delayHandlingOfDependerNotification = false
        s.suppressError = true
        s
    }
}

class EagerFalsePKESequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore()
        s.dependeeUpdateHandling = EagerDependeeUpdateHandling
        s.delayHandlingOfDependerNotification = false
        s.suppressError = true
        s
    }
}

class EagerTruePKESequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore()
        s.dependeeUpdateHandling = EagerDependeeUpdateHandling
        s.delayHandlingOfDependerNotification = true
        s.suppressError = true
        s
    }
}
