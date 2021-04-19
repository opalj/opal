package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;

public class InitializationInConstructor {
    //@Immutable
    @MutableField("")
    @MutableFieldReference("")
    private InitializationInConstructor child;
    public InitializationInConstructor(InitializationInConstructor parent) {
        parent.child = this;
        }
    }
