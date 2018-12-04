/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package seq

class TrueTrueTruePKESequentialPropertyStoreTest extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore()
        s.dependeeUpdateHandling = DependeeUpdateHandling(
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
        s.dependeeUpdateHandling = DependeeUpdateHandling(
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
        s.dependeeUpdateHandling = DependeeUpdateHandling(
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
        s.dependeeUpdateHandling = DependeeUpdateHandling(
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
        s.dependeeUpdateHandling = DependeeUpdateHandling(
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
        s.dependeeUpdateHandling = DependeeUpdateHandling(
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
        s.dependeeUpdateHandling = DependeeUpdateHandling(
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
        s.dependeeUpdateHandling = DependeeUpdateHandling(
            delayHandlingOfFinalDependeeUpdates = false,
            delayHandlingOfNonFinalDependeeUpdates = false
        )
        s.delayHandlingOfDependerNotification = false
        s.suppressError = true
        s
    }
}

