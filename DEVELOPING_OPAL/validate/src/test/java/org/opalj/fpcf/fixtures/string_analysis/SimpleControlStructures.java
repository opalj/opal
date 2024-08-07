/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string_analysis;

import org.opalj.fpcf.properties.string_analysis.*;

import java.util.Random;

/**
 * @see SimpleStringOps
 */
public class SimpleControlStructures {

    /**
     * Serves as the sink for string variables to be analyzed.
     */
    public void analyzeString(String s) {}

    @Dynamic(n = 0, levels = Level.TRUTH, value = "(^-?\\d+$|x)")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 1, levels = Level.TRUTH, value = "(42-42|x)")
    @Failure(n = 1, levels = Level.L0)
    public void ifElseWithStringBuilderWithIntExpr() {
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        int i = new Random().nextInt();
        if (i % 2 == 0) {
            sb1.append("x");
            sb2.append(42);
            sb2.append(-42);
        } else {
            sb1.append(i + 1);
            sb2.append("x");
        }
        analyzeString(sb1.toString());
        analyzeString(sb2.toString());
    }

    @PartiallyConstant(n = 0, levels = Level.TRUTH, value = "(3.142.71828|^-?\\d*\\.{0,1}\\d+$2.71828)")
    @Failure(n = 0, levels = Level.L0)
    public void ifElseWithStringBuilderWithFloatExpr() {
        StringBuilder sb1 = new StringBuilder();
        int i = new Random().nextInt();
        if (i % 2 == 0) {
            sb1.append(3.14);
        } else {
            sb1.append(new Random().nextFloat());
        }
        float e = (float) 2.71828;
        sb1.append(e);
        analyzeString(sb1.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(a|b)")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 1, levels = Level.TRUTH, value = "(ab|ac)")
    @Failure(n = 1, levels = Level.L0)
    public void ifElseWithStringBuilder() {
        StringBuilder sb1;
        StringBuilder sb2 = new StringBuilder("a");

        int i = new Random().nextInt();
        if (i % 2 == 0) {
            sb1 = new StringBuilder("a");
            sb2.append("b");
        } else {
            sb1 = new StringBuilder("b");
            sb2.append("c");
        }
        analyzeString(sb1.toString());
        analyzeString(sb2.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(abcd|axyz)")
    @Failure(n = 0, levels = Level.L0)
    public void ifElseWithStringBuilderWithMultipleAppends() {
        StringBuilder sb = new StringBuilder("a");
        int i = new Random().nextInt();
        if (i % 2 == 0) {
            sb.append("b");
            sb.append("c");
            sb.append("d");
        } else {
            sb.append("x");
            sb.append("y");
            sb.append("z");
        }
        analyzeString(sb.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(a|abcd|axyz)")
    @Failure(n = 0, levels = Level.L0)
    public void ifElseWithStringBuilderWithMultipleAppendsAndNonUsedElseIf() {
        StringBuilder sb = new StringBuilder("a");
        int i = new Random().nextInt();
        if (i % 3 == 0) {
            sb.append("b");
            sb.append("c");
            sb.append("d");
        } else if (i % 2 == 0) {
            System.out.println("something");
        } else {
            sb.append("x");
            sb.append("y");
            sb.append("z");
        }
        analyzeString(sb.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(a|ab)")
    @Failure(n = 0, levels = Level.L0)
    public void ifWithoutElse() {
        StringBuilder sb = new StringBuilder("a");
        int i = new Random().nextInt();
        if (i % 2 == 0) {
            sb.append("b");
        }
        analyzeString(sb.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "java.lang.Runtime")
    @Failure(n = 0, levels = Level.L0)
    public void ifConditionAppendsToString(String className) {
        StringBuilder sb = new StringBuilder();
        if (sb.append("java.lang.Runtime").toString().equals(className)) {
            System.out.println("Yep, got the correct class!");
        }
        analyzeString(sb.toString());
    }
}
