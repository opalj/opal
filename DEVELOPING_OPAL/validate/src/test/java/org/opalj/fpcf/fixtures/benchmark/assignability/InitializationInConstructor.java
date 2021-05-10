package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.AssignableFieldReference;

public class InitializationInConstructor {
    //@Immutable
    @MutableField("")
    @AssignableFieldReference("")
    private InitializationInConstructor child;
    public InitializationInConstructor(InitializationInConstructor parent) {
        parent.child = this;
        }
    }
