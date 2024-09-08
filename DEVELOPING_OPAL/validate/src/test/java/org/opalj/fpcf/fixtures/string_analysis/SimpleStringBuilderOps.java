/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string_analysis;

import org.opalj.fpcf.properties.string_analysis.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @see SimpleStringOps
 */
public class SimpleStringBuilderOps {

    /**
     * Serves as the sink for string variables to be analyzed.
     */
    public void analyzeString(String s) {}
    public void analyzeString(StringBuilder sb) {}

    @Constant(n = 0, levels = Level.TRUTH, value = "java.lang.String")
    @Failure(n = 0, levels = Level.L0)
    public void multipleDirectAppends() {
        StringBuilder sb = new StringBuilder("java");
        sb.append(".").append("lang").append(".").append("String");
        analyzeString(sb.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "SomeOther")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 1, levels = Level.TRUTH, value = "SomeOther")
    @Failure(n = 1, levels = Level.L0)
    @Constant(n = 1, levels = { Level.L1, Level.L2, Level.L3 }, value = "(Some|SomeOther)")
    public void stringValueOfWithStringBuilder() {
        StringBuilder sb = new StringBuilder("Some");
        sb.append("Other");
        analyzeString(String.valueOf(sb));

        analyzeString(sb);
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "Some")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 1, levels = Level.TRUTH, value = "Other")
    @Failure(n = 1, levels = Level.L0)
    public void stringBuilderBufferInitArguments() {
        StringBuilder sb = new StringBuilder("Some");
        analyzeString(sb.toString());

        StringBuffer sb2 = new StringBuffer("Other");
        analyzeString(sb2.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "java.lang.StringBuilder")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 1, levels = Level.TRUTH, value = "java.lang.StringBuilder")
    @Failure(n = 1, levels = Level.L0)
    public void simpleClearExamples() {
        StringBuilder sb1 = new StringBuilder("init_value:");
        sb1.setLength(0);
        sb1.append("java.lang.StringBuilder");

        StringBuilder sb2 = new StringBuilder("init_value:");
        System.out.println(sb2.toString());
        sb2 = new StringBuilder();
        sb2.append("java.lang.StringBuilder");

        analyzeString(sb1.toString());
        analyzeString(sb2.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(Goodbye|init_value:Hello, world!Goodbye)")
    @Failure(n = 0, levels = Level.L0)
    public void advancedClearExampleWithSetLength(int value) {
        StringBuilder sb = new StringBuilder("init_value:");
        if (value < 10) {
            sb.setLength(0);
        } else {
            sb.append("Hello, world!");
        }
        sb.append("Goodbye");
        analyzeString(sb.toString());
    }

    @Dynamic(n = 0, levels = Level.TRUTH, value = ".*")
    @Failure(n = 0, levels = Level.L0)
    @PartiallyConstant(n = 1, levels = Level.TRUTH, value = "(.*Goodbye|init_value:Hello, world!Goodbye)")
    @Failure(n = 1, levels = Level.L0)
    public void replaceExamples(int value) {
        StringBuilder sb1 = new StringBuilder("init_value");
        sb1.replace(0, 5, "replaced_value");
        analyzeString(sb1.toString());

        sb1 = new StringBuilder("init_value:");
        if (value < 10) {
            sb1.replace(0, value, "...");
        } else {
            sb1.append("Hello, world!");
        }
        sb1.append("Goodbye");
        analyzeString(sb1.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "B.")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 1, levels = Level.TRUTH, value = "java.langStringB.")
    @Failure(n = 1, levels = Level.L0)
    public void directAppendConcatsWith2ndStringBuilder() {
        StringBuilder sb = new StringBuilder("java");
        StringBuilder sb2 = new StringBuilder("B");
        sb.append('.').append("lang");
        sb2.append('.');
        sb.append("String");
        sb.append(sb2.toString());
        analyzeString(sb2.toString());
        analyzeString(sb.toString());
    }

    /**
     * Checks if the value of a string builder that depends on the complex construction of a second one can be determined.
     */
    @Constant(n = 0, levels = Level.TRUTH, value = "java.lang.(Object|Runtime)")
    @Failure(n = 0, levels = Level.L0)
    public void complexSecondStringBuilderRead(String className) {
        StringBuilder sbObj = new StringBuilder("Object");
        StringBuilder sbRun = new StringBuilder("Runtime");

        StringBuilder sb1 = new StringBuilder();
        if (sb1.length() == 0) {
            sb1.append(sbObj.toString());
        } else {
            sb1.append(sbRun.toString());
        }

        StringBuilder sb2 = new StringBuilder("java.lang.");
        sb2.append(sb1.toString());
        analyzeString(sb2.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(java.lang.Object|java.lang.Runtime)")
    @Failure(n = 0, levels = Level.L0)
    public void simpleSecondStringBuilderRead(String className) {
        StringBuilder sbObj = new StringBuilder("Object");
        StringBuilder sbRun = new StringBuilder("Runtime");

        StringBuilder sb1 = new StringBuilder("java.lang.");
        if (sb1.length() == 0) {
            sb1.append(sbObj.toString());
        } else {
            sb1.append(sbRun.toString());
        }

        analyzeString(sb1.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(Object|ObjectRuntime)")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 1, levels = Level.TRUTH, value = "(Runtime|RuntimeObject)")
    @Failure(n = 1, levels = Level.L0)
    public void crissCrossExample(String className) {
        StringBuilder sbObj = new StringBuilder("Object");
        StringBuilder sbRun = new StringBuilder("Runtime");

        if (className.length() == 0) {
            sbRun.append(sbObj.toString());
        } else {
            sbObj.append(sbRun.toString());
        }

        analyzeString(sbObj.toString());
        analyzeString(sbRun.toString());
    }

    @PartiallyConstant(n = 0, levels = Level.TRUTH, value = "File Content:.*", soundness = SoundnessMode.HIGH)
    @Failure(n = 0, levels = Level.L0)
    public void withUnknownAppendSource(String filename) throws IOException {
        StringBuilder sb = new StringBuilder("File Content:");
        String data = new String(Files.readAllBytes(Paths.get(filename)));
        sb.append(data);
        analyzeString(sb.toString());
    }
}
