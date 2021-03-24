/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@MutableClass("")
public class Singleton {

    //@Immutable
    @MutableField("")
    @MutableFieldReference("written by static initializer after the field becomes (indirectly) readable")
    private String name;

    //@Immutable
    @DeepImmutableField("")
    @ImmutableFieldReference("only initialized once by the constructor")
    private Object mutex = new Object();

    private Singleton() {
        this.name = "";
    }

    public String getName() {
        synchronized (mutex) {
            return name;
        }
    }

    // STATIC FUNCTIONALITY
    //@Immutable
    @ShallowImmutableField("")
    @ImmutableFieldReference("only set in the static initializer")
    private static Singleton theInstance;

    static {
        theInstance = new Singleton();
        theInstance.name = "The Singleton Instance";
    }

    public static Singleton getInstance() {
        return theInstance;
    }

}
