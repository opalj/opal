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

class TrueTrueTrueSequentialPropertyStoreTest extends PropertyStoreTest {

    def createPropertyStore(): PropertyStore = {
        val s = SequentialPropertyStore()

        s.debug = true

        s.delayHandlingOfFinalDependeeUpdates = true
        s.delayHandlingOfNonFinalDependeeUpdates = true
        s.delayHandlingOfDependerNotification = true

        s
    }

}

class FalseTrueTrueSequentialPropertyStoreTest extends PropertyStoreTest {

    def createPropertyStore(): PropertyStore = {
        val s = SequentialPropertyStore()

        s.debug = true

        s.delayHandlingOfFinalDependeeUpdates = false
        s.delayHandlingOfNonFinalDependeeUpdates = true
        s.delayHandlingOfDependerNotification = true
        s
    }
}
class TrueFalseTrueSequentialPropertyStoreTest extends PropertyStoreTest {

    def createPropertyStore(): PropertyStore = {
        val s = SequentialPropertyStore()

        s.debug = true

        s.delayHandlingOfFinalDependeeUpdates = true
        s.delayHandlingOfNonFinalDependeeUpdates = false
        s.delayHandlingOfDependerNotification = true
        s
    }
}
class TrueTrueFalseSequentialPropertyStoreTest extends PropertyStoreTest {

    def createPropertyStore(): PropertyStore = {
        val s = SequentialPropertyStore()

        s.debug = true

        s.delayHandlingOfFinalDependeeUpdates = true
        s.delayHandlingOfNonFinalDependeeUpdates = true
        s.delayHandlingOfDependerNotification = false
        s
    }
}

class FalseFalseTrueSequentialPropertyStoreTest extends PropertyStoreTest {

    def createPropertyStore(): PropertyStore = {
        val s = SequentialPropertyStore()

        s.debug = true

        s.delayHandlingOfFinalDependeeUpdates = false
        s.delayHandlingOfNonFinalDependeeUpdates = false
        s.delayHandlingOfDependerNotification = true
        s
    }
}

class FalseTrueFalseSequentialPropertyStoreTest extends PropertyStoreTest {

    def createPropertyStore(): PropertyStore = {
        val s = SequentialPropertyStore()

        s.debug = true

        s.delayHandlingOfFinalDependeeUpdates = false
        s.delayHandlingOfNonFinalDependeeUpdates = true
        s.delayHandlingOfDependerNotification = false
        s
    }
}

class TrueFalseFalseSequentialPropertyStoreTest extends PropertyStoreTest {

    def createPropertyStore(): PropertyStore = {
        val s = SequentialPropertyStore()

        s.debug = true

        s.delayHandlingOfFinalDependeeUpdates = true
        s.delayHandlingOfNonFinalDependeeUpdates = false
        s.delayHandlingOfDependerNotification = false
        s
    }
}

class FalseFalseFalseSequentialPropertyStoreTest extends PropertyStoreTest {

    def createPropertyStore(): PropertyStore = {
        val s = SequentialPropertyStore()

        s.debug = true

        s.delayHandlingOfFinalDependeeUpdates = false
        s.delayHandlingOfNonFinalDependeeUpdates = false
        s.delayHandlingOfDependerNotification = false
        s
    }
}
