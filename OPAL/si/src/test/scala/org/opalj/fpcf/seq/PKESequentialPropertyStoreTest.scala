/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package seq

class LIFOTaskskManagePKESequentialPropertyStoreTest
    extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore("LIFO")()
        s.suppressError = true
        s
    }

}

class FIFOTaskskManagePKESequentialPropertyStoreTest
    extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore("FIFO")()
        s.suppressError = true
        s
    }
}

class ManyDependenciesLastTaskskManagePKESequentialPropertyStoreTest
    extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore("ManyDirectDependenciesLast")()
        s.suppressError = true
        s
    }
}

class ManyDependersLastTaskskManagePKESequentialPropertyStoreTest
    extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore("ManyDirectDependersLast")()
        s.suppressError = true
        s
    }
}

class ManyDependeesOfDependersLastTaskskManagePKESequentialPropertyStoreTest
    extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore("ManyDependeesOfDirectDependersLast")()
        s.suppressError = true
        s
    }
}

class ManyDependeesAndDependersOfDependersLastTaskskManagePKESequentialPropertyStoreTest
    extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore("ManyDependeesAndDependersOfDirectDependersLast")()
        s.suppressError = true
        s
    }
}