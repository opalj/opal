package org.opalj.fpcf.fixtures.benchmark.assignability;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;

public class InitializationInConstructor {
    //@Immutable
    @MutableField("")
    @AssignableField("")
    private InitializationInConstructor child;
    public InitializationInConstructor(InitializationInConstructor parent) {
        parent.child = this;
        }
    }
