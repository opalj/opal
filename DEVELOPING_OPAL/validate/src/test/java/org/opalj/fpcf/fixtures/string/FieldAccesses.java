/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string;

import org.opalj.fpcf.properties.string.*;

import java.util.Random;

/**
 * Various tests that test general compatibility with the field access information FPCF property, e.g. being able to
 * analyze field reads and writes across method boundaries.
 *
 * @see SimpleStringOps
 */
public class FieldAccesses {

    protected String nonFinalNonStaticField = "private l0 non-final string field";
    public static String nonFinalStaticField = "will not be revealed here";
    public static final String finalStaticField = "mine";
    private String fieldWithSelfInit = "init field value";
    private static final String fieldWithSelfInitWithComplexInit;
    private String fieldWithConstructorInit;
    private float fieldWithConstructorParameterInit;
    private String writeInSameMethodField;
    private String noWriteField;
    private Object unsupportedTypeField;

    static {
        if (new Random().nextBoolean()) {
            fieldWithSelfInitWithComplexInit = "Impl_Stub_1";
        } else {
            fieldWithSelfInitWithComplexInit = "Impl_Stub_2";
        }
    }

    public FieldAccesses(float e) {
        fieldWithConstructorInit = "initialized by constructor";
        fieldWithConstructorParameterInit = e;
    }

    /**
     * Serves as the sink for string variables to be analyzed.
     */
    public void analyzeString(String s) {}

    @Constant(n = 0, levels = Level.TRUTH, value = "private l0 non-final string field")
    @Failure(n = 0, levels = { Level.L0, Level.L1, Level.L2 })
    public void nonFinalFieldRead() {
        analyzeString(nonFinalNonStaticField);
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "will not be revealed here")
    @Failure(n = 0, levels = { Level.L0, Level.L1, Level.L2 })
    public void nonFinalStaticFieldRead() {
        analyzeString(nonFinalStaticField);
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "Field Value:mine")
    @Failure(n = 0, levels = Level.L0)
    public void publicFinalStaticFieldRead() {
        StringBuilder sb = new StringBuilder("Field Value:");
        System.out.println(sb);
        sb.append(finalStaticField);
        analyzeString(sb.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "init field value")
    @Failure(n = 0, levels = { Level.L0, Level.L1, Level.L2 })
    public void fieldWithInitRead() {
        analyzeString(fieldWithSelfInit.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(Impl_Stub_1|Impl_Stub_2)")
    @Failure(n = 0, levels = { Level.L0, Level.L1, Level.L2 })
    public void fieldWithInitWithOutOfScopeRead() {
        analyzeString(fieldWithSelfInitWithComplexInit);
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "initialized by constructor")
    @Failure(n = 0, levels = { Level.L0, Level.L1, Level.L2 })
    public void fieldInitByConstructorRead() {
        analyzeString(fieldWithConstructorInit.toString());
    }

    @Dynamic(n = 0, levels = Level.TRUTH, value = "^-?\\d*\\.{0,1}\\d+$")
    @Failure(n = 0, levels = Level.L0)
    @Failure(n = 0, levels = Level.L1, domains = DomainLevel.L1)
    @Invalid(n = 0, levels = Level.L1, domains = DomainLevel.L2, soundness = SoundnessMode.LOW)
    @Dynamic(n = 0, levels = Level.L1, domains = DomainLevel.L2, soundness = SoundnessMode.HIGH,
        value = "^-?\\d*\\.{0,1}\\d+$", reason = "the field value is inlined using L2 domains")
    @Invalid(n = 0, levels = { Level.L2, Level.L3 }, soundness = SoundnessMode.LOW)
    public void fieldInitByConstructorParameter() {
        analyzeString(new StringBuilder().append(fieldWithConstructorParameterInit).toString());
    }

    // Contains a field write in the same method which cannot be captured by flow functions
    @Constant(n = 0, levels = Level.TRUTH, value = "(some value|^null$)")
    @Failure(n = 0, levels = { Level.L0, Level.L1, Level.L2 })
    @Constant(n = 0, levels = Level.L3, soundness = SoundnessMode.LOW, value = "some value")
    @Dynamic(n = 0, levels = Level.L3, soundness = SoundnessMode.HIGH, value = ".*")
    public void fieldWriteInSameMethod() {
        writeInSameMethodField = "some value";
        analyzeString(writeInSameMethodField);
    }

    @Dynamic(n = 0, levels = Level.TRUTH, value = "(.*|^null$)")
    @Failure(n = 0, levels = { Level.L0, Level.L1, Level.L2 })
    @Constant(n = 0, levels = Level.L3, soundness = SoundnessMode.LOW, value = "^null$")
    public void fieldWithNoWriteTest() {
        analyzeString(noWriteField);
    }

    @Failure(n = 0, levels = { Level.L0, Level.L1, Level.L2, Level.L3 })
    public void nonSupportedFieldTypeRead() {
        analyzeString(unsupportedTypeField.toString());
    }
}
