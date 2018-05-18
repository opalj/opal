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

class TrueTrueTrueEPKSequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = EPKSequentialPropertyStore()
        s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
            delayHandlingOfFinalDependeeUpdates = true,
            delayHandlingOfNonFinalDependeeUpdates = true
        )
        s.delayHandlingOfDependerNotification = true
        s
    }

}

class FalseTrueTrueEPKSequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = EPKSequentialPropertyStore()
        s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
            delayHandlingOfFinalDependeeUpdates = false,
            delayHandlingOfNonFinalDependeeUpdates = true
        )
        s.delayHandlingOfDependerNotification = true
        s
    }
}

class TrueFalseTrueEPKSequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = EPKSequentialPropertyStore()
        s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
            delayHandlingOfFinalDependeeUpdates = true,
            delayHandlingOfNonFinalDependeeUpdates = false
        )
        s.delayHandlingOfDependerNotification = true
        s
    }
}
class TrueTrueFalseEPKSequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = EPKSequentialPropertyStore()
        s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
            delayHandlingOfFinalDependeeUpdates = true,
            delayHandlingOfNonFinalDependeeUpdates = true
        )
        s.delayHandlingOfDependerNotification = false
        s
    }
}

class FalseFalseTrueEPKSequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = EPKSequentialPropertyStore()
        s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
            delayHandlingOfFinalDependeeUpdates = false,
            delayHandlingOfNonFinalDependeeUpdates = false
        )
        s.delayHandlingOfDependerNotification = true
        s
    }
}

class FalseTrueFalseEPKSequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = EPKSequentialPropertyStore()
        s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
            delayHandlingOfFinalDependeeUpdates = false,
            delayHandlingOfNonFinalDependeeUpdates = true
        )
        s.delayHandlingOfDependerNotification = false
        s
    }
}

class TrueFalseFalseEPKSequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = EPKSequentialPropertyStore()
        s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
            delayHandlingOfFinalDependeeUpdates = true,
            delayHandlingOfNonFinalDependeeUpdates = false
        )
        s.delayHandlingOfDependerNotification = false
        s
    }
}

class FalseFalseFalseEPKSequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = EPKSequentialPropertyStore()
        s.dependeeUpdateHandling = LazyDependeeUpdateHandling(
            delayHandlingOfFinalDependeeUpdates = false,
            delayHandlingOfNonFinalDependeeUpdates = false
        )
        s.delayHandlingOfDependerNotification = false
        s
    }
}

class EagerFalseEPKSequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = EPKSequentialPropertyStore()
        s.dependeeUpdateHandling = EagerDependeeUpdateHandling
        s.delayHandlingOfDependerNotification = false
        s
    }
}

class EagerTrueEPKSequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = EPKSequentialPropertyStore()
        s.dependeeUpdateHandling = EagerDependeeUpdateHandling
        s.delayHandlingOfDependerNotification = true
        s
    }
}
