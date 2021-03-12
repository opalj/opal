/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.lazy_initialization;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@DeepImmutableClass("")
public class DoubleCheckedLockingClassWithStaticFieldsDeep {

    @LazyInitializedThreadSafeFieldReference("standard dcl pattern within a static method")
    private static Object instance;

    public static Object getInstance() {
        if (instance == null) {
            synchronized (Object.class) {
                if (instance == null) {
                    instance = new Object();
                }
            }
        }
        return instance;
    }
}
