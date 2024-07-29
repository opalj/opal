/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string_analysis;

import org.opalj.fpcf.properties.string_analysis.*;

import javax.management.remote.rmi.RMIServer;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * @see SimpleStringOps
 */
public class External {

    protected String nonFinalNonStaticField = "private l0 non-final string field";
    public static String nonFinalStaticField = "will not be revealed here";
    public static final String finalStaticField = "mine";
    private String fieldWithSelfInit = "init field value";
    private static final String fieldWithSelfInitWithOutOfScopeCall = RMIServer.class.getName() + "Impl_Stub";
    private String fieldWithConstructorInit;
    private float fieldWithConstructorParameterInit;
    private String writeInSameMethodField;
    private String noWriteField;
    private Object unsupportedTypeField;

    public External(float e) {
        fieldWithConstructorInit = "initialized by constructor";
        fieldWithConstructorParameterInit = e;
    }

    /**
     * Serves as the sink for string variables to be analyzed.
     */
    public void analyzeString(String s) {}

    @Constant(n = 0, levels = Level.TRUTH, value = "Field Value:private l0 non-final string field")
    @Invalid(n = 0, levels = Level.L0, soundness = SoundnessMode.LOW)
    @PartiallyConstant(n = 0, levels = Level.L0, soundness = SoundnessMode.HIGH, value = "Field Value:.*")
    public void nonFinalFieldRead() {
        StringBuilder sb = new StringBuilder("Field Value:");
        System.out.println(sb);
        sb.append(nonFinalNonStaticField);
        analyzeString(sb.toString());
    }

    @Constant(n = 0, value = "will not be revealed here")
    @Failure(n = 0, levels = Level.L0)
    public void nonFinalStaticFieldRead() {
        analyzeString(nonFinalStaticField);
    }

    @Constant(n = 0, value = "Field Value:mine")
    public void publicFinalStaticFieldRead() {
        StringBuilder sb = new StringBuilder("Field Value:");
        System.out.println(sb);
        sb.append(finalStaticField);
        analyzeString(sb.toString());
    }

    @Constant(n = 0, value = "init field value")
    @Failure(n = 0, levels = Level.L0)
    public void fieldWithInitRead() {
        analyzeString(fieldWithSelfInit.toString());
    }

    @PartiallyConstant(n = 0, soundness = SoundnessMode.HIGH, value = ".*Impl_Stub")
    @Failure(n = 0, levels = Level.L0)
    public void fieldWithInitWithOutOfScopeRead() {
        analyzeString(fieldWithSelfInitWithOutOfScopeCall);
    }

    @Constant(n = 0, value = "initialized by constructor")
    @Failure(n = 0, levels = Level.L0)
    public void fieldInitByConstructorRead() {
        analyzeString(fieldWithConstructorInit.toString());
    }

    @Dynamic(n = 0, levels = Level.TRUTH, value = "^-?\\d*\\.{0,1}\\d+$")
    @Failure(n = 0, levels = Level.L0, domains = DomainLevel.L1)
    @Invalid(n = 0, levels = Level.L0, domains = DomainLevel.L2, soundness = SoundnessMode.LOW)
    @Dynamic(n = 0, levels = Level.L0, domains = DomainLevel.L2, soundness = SoundnessMode.HIGH,
        value = "^-?\\d*\\.{0,1}\\d+$", reason = "the field value is inlined using L2 domains")
    public void fieldInitByConstructorParameter() {
        analyzeString(new StringBuilder().append(fieldWithConstructorParameterInit).toString());
    }

    // Contains a field write in the same method which cannot be captured by flow functions
    @Constant(n = 0, levels = Level.TRUTH, value = "(some value|^null$)")
    @Failure(n = 0, levels = Level.L0)
    @Dynamic(n = 0, levels = Level.L1, value = ".*")
    public void fieldWriteInSameMethod() {
        writeInSameMethodField = "some value";
        analyzeString(writeInSameMethodField);
    }

    @Dynamic(n = 0, levels = Level.TRUTH, value = "(.*|^null$)")
    @Failure(n = 0, levels = Level.L0)
    public void fieldWithNoWriteTest() {
        analyzeString(noWriteField);
    }

    @Failure(n = 0, levels = { Level.L0, Level.L1 })
    public void nonSupportedFieldTypeRead() {
        analyzeString(unsupportedTypeField.toString());
    }

    @Dynamic(n = 0, value = ".*")
    @Dynamic(n = 1, value = ".*")
    @PartiallyConstant(n = 2, value = "value=.*")
    @PartiallyConstant(n = 3, value = "value=.*.*")
    public void parameterRead(String stringValue, StringBuilder sbValue) {
        analyzeString(stringValue);
        analyzeString(sbValue.toString());

        StringBuilder sb = new StringBuilder("value=");
        System.out.println(sb.toString());
        sb.append(stringValue);
        analyzeString(sb.toString());

        sb.append(sbValue.toString());
        analyzeString(sb.toString());
    }

    /**
     * Methods are called that return a string but are not within this project => cannot / will not interpret
     */
    @Dynamic(n = 0, levels = Level.TRUTH, value = "(.*)*")
    @Dynamic(n = 0, levels = { Level.L0, Level.L1 }, value = ".*")
    @Invalid(n = 1, levels = Level.TRUTH, soundness = SoundnessMode.LOW)
    @Dynamic(n = 1, levels = { Level.L0, Level.L1 }, soundness = SoundnessMode.LOW, value = ".*")
    @Dynamic(n = 1, levels = Level.TRUTH, soundness = SoundnessMode.HIGH, value = ".*")
    public void methodsOutOfScopeTest() throws FileNotFoundException {
        File file = new File("my-file.txt");
        Scanner sc = new Scanner(file);
        StringBuilder sb = new StringBuilder();
        while (sc.hasNextLine()) {
            sb.append(sc.nextLine());
        }
        analyzeString(sb.toString());

        analyzeString(System.clearProperty("os.version"));
    }
}
