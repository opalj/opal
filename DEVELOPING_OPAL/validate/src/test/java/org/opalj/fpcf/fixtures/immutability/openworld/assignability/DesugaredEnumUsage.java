/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.assignability;

import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;

/**
 * A case of a "desugared enum" usage. The Java 8 (bytecode 52.0) compiler generates a switch table in a synthetic inner
 * class that is populated in a static initializer. As such, the enum values are static fields that are written in their
 * own static initializer and read in another static initializer.
 */
public class DesugaredEnumUsage {

    enum MyDesugaredEnum {
        @NonAssignableField("Static fields generated to hold enum value singletons are final")
        SOME_VALUE,

        @NonAssignableField("Static fields generated to hold enum value singletons are final")
        SOME_OTHER_VALUE
    }

    void iDesugarYourEnum(MyDesugaredEnum e) {
        switch (e) {
            case SOME_VALUE:
                System.out.println("Some value found!");
            case SOME_OTHER_VALUE:
                System.out.println("Some other value found!");
            default:
                System.err.println("No value found!");
        }
    }
}

