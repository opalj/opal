/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package seq

abstract class PKESequentialPropertyStoreWithDebugging
    extends PropertyStoreTestWithDebugging[PKESequentialPropertyStore]

class LIFOTasksManagerPKESequentialPropertyStoreTest
    extends PKESequentialPropertyStoreWithDebugging {

    def createPropertyStore(): PKESequentialPropertyStore = {
        val s = PKESequentialPropertyStore("LIFO", 0)()
        s.suppressError = true
        s
    }

}

class FIFOTasksManagerPKESequentialPropertyStoreTest
    extends PKESequentialPropertyStoreWithDebugging {

    def createPropertyStore(): PKESequentialPropertyStore = {
        val s = PKESequentialPropertyStore("FIFO", 4)()
        s.suppressError = true
        s
    }
}

class ManyDependenciesLastTasksManagerPKESequentialPropertyStoreTest
    extends PKESequentialPropertyStoreWithDebugging {

    def createPropertyStore(): PKESequentialPropertyStore = {
        val s = PKESequentialPropertyStore("ManyDirectDependenciesLast", 8)()
        s.suppressError = true
        s
    }
}

class ManyDependersLastTasksManagerPKESequentialPropertyStoreTest
    extends PKESequentialPropertyStoreWithDebugging {

    def createPropertyStore(): PKESequentialPropertyStore = {
        val s = PKESequentialPropertyStore("ManyDirectDependersLast", 16)()
        s.suppressError = true
        s
    }
}

class ManyDependeesOfDependersLastTasksManagerPKESequentialPropertyStoreTest
    extends PKESequentialPropertyStoreWithDebugging {

    def createPropertyStore(): PKESequentialPropertyStore = {
        val s = PKESequentialPropertyStore("ManyDependeesOfDirectDependersLast", 32)()
        s.suppressError = true
        s
    }
}

class ManyDependeesAndDependersOfDependersLastTasksManagerPKESequentialPropertyStoreTest
    extends PKESequentialPropertyStoreWithDebugging {

    def createPropertyStore(): PKESequentialPropertyStore = {
        val s = PKESequentialPropertyStore("ManyDependeesAndDependersOfDirectDependersLast", 128)()
        s.suppressError = true
        s
    }
}
