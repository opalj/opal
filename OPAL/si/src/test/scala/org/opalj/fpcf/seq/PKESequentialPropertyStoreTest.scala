/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package seq

abstract class PKESequentialPropertyStoreWithDebugging
    extends PropertyStoreTestWithDebugging[PKESequentialPropertyStore]

class LIFOTaskskManagePKESequentialPropertyStoreTest
    extends PKESequentialPropertyStoreWithDebugging {

    def createPropertyStore(): PKESequentialPropertyStore = {
        val s = PKESequentialPropertyStore("LIFO", 0)()
        s.suppressError = true
        s
    }

}

class FIFOTaskskManagePKESequentialPropertyStoreTest
    extends PKESequentialPropertyStoreWithDebugging {

    def createPropertyStore(): PKESequentialPropertyStore = {
        val s = PKESequentialPropertyStore("FIFO", 4)()
        s.suppressError = true
        s
    }
}

class ManyDependenciesLastTaskskManagePKESequentialPropertyStoreTest
    extends PKESequentialPropertyStoreWithDebugging {

    def createPropertyStore(): PKESequentialPropertyStore = {
        val s = PKESequentialPropertyStore("ManyDirectDependenciesLast", 8)()
        s.suppressError = true
        s
    }
}

class ManyDependersLastTaskskManagePKESequentialPropertyStoreTest
    extends PKESequentialPropertyStoreWithDebugging {

    def createPropertyStore(): PKESequentialPropertyStore = {
        val s = PKESequentialPropertyStore("ManyDirectDependersLast", 16)()
        s.suppressError = true
        s
    }
}

class ManyDependeesOfDependersLastTaskskManagePKESequentialPropertyStoreTest
    extends PKESequentialPropertyStoreWithDebugging {

    def createPropertyStore(): PKESequentialPropertyStore = {
        val s = PKESequentialPropertyStore("ManyDependeesOfDirectDependersLast", 32)()
        s.suppressError = true
        s
    }
}

class ManyDependeesAndDependersOfDependersLastTaskskManagePKESequentialPropertyStoreTest
    extends PKESequentialPropertyStoreWithDebugging {

    def createPropertyStore(): PKESequentialPropertyStore = {
        val s = PKESequentialPropertyStore("ManyDependeesAndDependersOfDirectDependersLast", 128)()
        s.suppressError = true
        s
    }
}
