/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
        s.suppressError = true
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
        s.suppressError = true
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
        s.suppressError = true
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
        s.suppressError = true
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
        s.suppressError = true
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
        s.suppressError = true
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
        s.suppressError = true
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
        s.suppressError = true
        s
    }
}

class EagerFalseEPKSequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = EPKSequentialPropertyStore()
        s.dependeeUpdateHandling = EagerDependeeUpdateHandling
        s.delayHandlingOfDependerNotification = false
        s.suppressError = true
        s
    }
}

class EagerTrueEPKSequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = EPKSequentialPropertyStore()
        s.dependeeUpdateHandling = EagerDependeeUpdateHandling
        s.delayHandlingOfDependerNotification = true
        s.suppressError = true
        s
    }
}
