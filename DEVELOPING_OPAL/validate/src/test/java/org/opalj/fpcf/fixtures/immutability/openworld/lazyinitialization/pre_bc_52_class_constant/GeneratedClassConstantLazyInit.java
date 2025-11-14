/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.lazyinitialization.pre_bc_52_class_constant;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.LazilyInitializedField;
import org.opalj.fpcf.properties.immutability.field_assignability.UnsafelyLazilyInitializedField;
import org.opalj.tac.fpcf.analyses.fieldassignability.L2FieldAssignabilityAnalysis;

/**
 * Contains lazy initializations of class constants as produced by compilers before class literal support was officially
 * introduced, i.e. before Java 5 (Bytecode version 49.0). Before class literals became part of the constant pool, they
 * used a synthetic singleton pattern, with inlined lazy initialization at the usage site.
 */
public class GeneratedClassConstantLazyInit {

    private class ClassA {}

    static Class<ClassA> class$(String className) {
        try {
            return (Class<ClassA>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @LazilyInitializedField(value = "A well behaved class loader returns singleton class instances (see JVM spec 5.3), and static initializers are inherently thread safe",
            analyses = {})
    @AssignableField(value = "The analysis only recognizes lazy init patterns in instance methods",
            analyses = { L2FieldAssignabilityAnalysis.class })
    private static Class<ClassA> class$lazyInitInStaticInitializer;

    static {
        Class<ClassA> instance = class$lazyInitInStaticInitializer == null
                ? (class$lazyInitInStaticInitializer = class$("string1"))
                : class$lazyInitInStaticInitializer;
    }

    @LazilyInitializedField(value = "A well behaved class loader returns singleton class instances (see JVM spec 5.3)",
            analyses = {})
    @UnsafelyLazilyInitializedField(value = "The analysis does currently not incorporate singleton information",
            analyses = { L2FieldAssignabilityAnalysis.class })
    private static Class<ClassA> class$lazyInitInInstanceMethod;

    void iUseClassConstantsOnce() {
        Class<ClassA> instance = class$lazyInitInInstanceMethod == null
                ? (class$lazyInitInInstanceMethod = class$("string2"))
                : class$lazyInitInInstanceMethod;
    }

    @LazilyInitializedField(value = "A well behaved class loader returns singleton class instances (see JVM spec 5.3)",
            analyses = {})
    @AssignableField(value = "Multiple lazy initialization patterns for the same field are not supported",
            analyses = { L2FieldAssignabilityAnalysis.class })
    private static Class<ClassA> class$multiPlaceLazyInit;

    void iUseClassConstantsTwice() {
        Class<ClassA> instance = class$multiPlaceLazyInit == null
                ? (class$multiPlaceLazyInit = class$("string3"))
                : class$multiPlaceLazyInit;

        Class<ClassA> other = class$multiPlaceLazyInit == null
                ? (class$multiPlaceLazyInit = class$("string4"))
                : class$multiPlaceLazyInit;
    }
}
