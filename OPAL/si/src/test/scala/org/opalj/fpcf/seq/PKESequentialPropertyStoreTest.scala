/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package seq

class LIFOTaskskManagePKESequentialPropertyStoreTest
    extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore("LIFO", 0)()
        s.suppressError = true
        s
    }

}

class FIFOTaskskManagePKESequentialPropertyStoreTest
    extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore("FIFO", 4)()
        s.suppressError = true
        s
    }
}

class ManyDependenciesLastTaskskManagePKESequentialPropertyStoreTest
    extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore("ManyDirectDependenciesLast", 8)()
        s.suppressError = true
        s
    }
}

class ManyDependersLastTaskskManagePKESequentialPropertyStoreTest
    extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore("ManyDirectDependersLast", 16)()
        s.suppressError = true
        s
    }
}

class ManyDependeesOfDependersLastTaskskManagePKESequentialPropertyStoreTest
    extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore("ManyDependeesOfDirectDependersLast", 32)()
        s.suppressError = true
        s
    }
}

class ManyDependeesAndDependersOfDependersLastTaskskManagePKESequentialPropertyStoreTest
    extends PropertyStoreTestWithDebugging {

    def createPropertyStore(): PropertyStore = {
        val s = PKESequentialPropertyStore("ManyDependeesAndDependersOfDirectDependersLast", 128)()
        s.suppressError = true
        s
    }
}