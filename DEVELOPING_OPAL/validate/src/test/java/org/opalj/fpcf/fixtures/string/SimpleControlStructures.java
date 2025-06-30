/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string;

import org.opalj.fpcf.properties.string.*;

import java.util.Random;

/**
 * Various tests that test compatibility of the data flow analysis with simple control structures like if-statements.
 *
 * @see SimpleStringOps
 */
public class SimpleControlStructures {

    /**
     * Serves as the sink for string variables to be analyzed.
     */
    public void analyzeString(String s) {}

    @Dynamic(sinkIndex = 0, levels = Level.TRUTH, value = "(^-?\\d+$|x)")
    @Failure(sinkIndex = 0, levels = Level.L0)
    @Constant(sinkIndex = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.LOW, value = "x")
    @Constant(sinkIndex = 1, levels = Level.TRUTH, value = "(42-42|x)")
    @Failure(sinkIndex = 1, levels = Level.L0)
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

    @PartiallyConstant(sinkIndex = 0, levels = Level.TRUTH, value = "(3.142.71828|^-?\\d*\\.{0,1}\\d+$2.71828)")
    @Failure(sinkIndex = 0, levels = Level.L0)
    @Constant(sinkIndex = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.LOW, value = "3.142.71828")
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

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "(a|b)")
    @Failure(sinkIndex = 0, levels = Level.L0)
    @Constant(sinkIndex = 1, levels = Level.TRUTH, value = "(ab|ac)")
    @Failure(sinkIndex = 1, levels = Level.L0)
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

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "(abcd|axyz)")
    @Failure(sinkIndex = 0, levels = Level.L0)
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

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "(a|abcd|axyz)")
    @Failure(sinkIndex = 0, levels = Level.L0)
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

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "(a|ab)")
    @Failure(sinkIndex = 0, levels = Level.L0)
    public void ifWithoutElse() {
        StringBuilder sb = new StringBuilder("a");
        int i = new Random().nextInt();
        if (i % 2 == 0) {
            sb.append("b");
        }
        analyzeString(sb.toString());
    }

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "java.lang.Runtime")
    @Failure(sinkIndex = 0, levels = Level.L0)
    public void ifConditionAppendsToString(String className) {
        StringBuilder sb = new StringBuilder();
        if (sb.append("java.lang.Runtime").toString().equals(className)) {
            System.out.println("Yep, got the correct class!");
        }
        analyzeString(sb.toString());
    }

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "(a|ab|ac)")
    @Failure(sinkIndex = 0, levels = Level.L0)
    public void switchRelevantAndIrrelevant(int value) {
        StringBuilder sb = new StringBuilder("a");
        switch (value) {
        case 0:
            sb.append("b");
            break;
        case 1:
            sb.append("c");
            break;
        case 3:
            break;
        case 4:
            break;
        }
        analyzeString(sb.toString());
    }

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "(a|ab|ac|ad)")
    @Failure(sinkIndex = 0, levels = Level.L0)
    public void switchRelevantAndIrrelevantWithRelevantDefault(int value) {
        StringBuilder sb = new StringBuilder("a");
        switch (value) {
        case 0:
            sb.append("b");
            break;
        case 1:
            sb.append("c");
            break;
        case 2:
            break;
        case 3:
            break;
        default:
            sb.append("d");
            break;
        }
        analyzeString(sb.toString());
    }

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "(a|ab|ac)")
    @Failure(sinkIndex = 0, levels = Level.L0)
    public void switchRelevantAndIrrelevantWithIrrelevantDefault(int value) {
        StringBuilder sb = new StringBuilder("a");
        switch (value) {
        case 0:
            sb.append("b");
            break;
        case 1:
            sb.append("c");
            break;
        case 2:
            break;
        case 3:
            break;
        default:
            break;
        }
        analyzeString(sb.toString());
    }

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "(ab|ac|ad)")
    @Failure(sinkIndex = 0, levels = Level.L0)
    public void switchRelevantWithRelevantDefault(int value) {
        StringBuilder sb = new StringBuilder("a");
        switch (value) {
        case 0:
            sb.append("b");
            break;
        case 1:
            sb.append("c");
            break;
        default:
            sb.append("d");
            break;
        }
        analyzeString(sb.toString());
    }

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "(a|ab|ac|ad|af)")
    @Failure(sinkIndex = 0, levels = Level.L0)
    public void switchNestedNoNestedDefault(int value, int value2) {
        StringBuilder sb = new StringBuilder("a");
        switch (value) {
        case 0:
            sb.append("b");
            break;
        case 1:
            switch (value2) {
            case 0:
                sb.append("c");
                break;
            case 1:
                sb.append("d");
                break;
            }
            break;
        default:
            sb.append("f");
            break;
        }
        analyzeString(sb.toString());
    }

    @Constant(sinkIndex = 0, levels = Level.TRUTH, value = "(ab|ac|ad|ae|af)")
    @Failure(sinkIndex = 0, levels = Level.L0)
    public void switchNestedWithNestedDefault(int value, int value2) {
        StringBuilder sb = new StringBuilder("a");
        switch (value) {
        case 0:
            sb.append("b");
            break;
        case 1:
            switch (value2) {
            case 0:
                sb.append("c");
                break;
            case 1:
                sb.append("d");
                break;
            default:
                sb.append("e");
                break;
            }
            break;
        default:
            sb.append("f");
            break;
        }
        analyzeString(sb.toString());
    }
}
