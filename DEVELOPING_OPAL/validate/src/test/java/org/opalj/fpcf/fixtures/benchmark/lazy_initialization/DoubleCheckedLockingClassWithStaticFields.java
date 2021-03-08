/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.lazy_initialization;

import org.opalj.fpcf.properties.immutability.references.LazyInitializedThreadSafeFieldReference;

class DoubleCheckedLockingClassWithStaticFields {

    @LazyInitializedThreadSafeFieldReference("standard dcl pattern within a static method")
    private static DoubleCheckedLockingClassWithStaticFields instance;

    public static DoubleCheckedLockingClassWithStaticFields getInstance() {
        if (instance == null) {
            synchronized (DoubleCheckedLockingClassWithStaticFields.class) {
                if (instance == null) {
                    instance = new DoubleCheckedLockingClassWithStaticFields();
                }
            }
        }
        return instance;
    }
}
