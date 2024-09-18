/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string;

import org.opalj.fpcf.properties.string_analysis.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Random;

/**
 * Various tests that contain some kind of loops which modify string variables, requiring data flow analysis to resolve
 * these values or at least approximate them. Currently, the string analysis either only interprets the loop body once
 * (in low-soundness mode) or over-approximates with "any string" (in high-soundness mode).
 *
 * @see SimpleStringOps
 */
public class Loops {

    /**
     * Serves as the sink for string variables to be analyzed.
     */
    public void analyzeString(String s) {}

    /**
     * Simple for loops with known and unknown bounds. Note that no analysis supports loops yet.
     */
    @PartiallyConstant(n = 0, levels = Level.TRUTH, value = "a(b)*")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.LOW, value = "ab")
    @Dynamic(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.HIGH, value = ".*")
    @PartiallyConstant(n = 1, levels = Level.TRUTH, value = "a(b)*")
    @Failure(n = 1, levels = Level.L0)
    @Constant(n = 1, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.LOW, value = "ab")
    @Dynamic(n = 1, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.HIGH, value = ".*")
    public void simpleForLoopWithKnownBounds() {
        StringBuilder sb = new StringBuilder("a");
        for (int i = 0; i < 10; i++) {
            sb.append("b");
        }
        analyzeString(sb.toString());

        int limit = new Random().nextInt();
        sb = new StringBuilder("a");
        for (int i = 0; i < limit; i++) {
            sb.append("b");
        }
        analyzeString(sb.toString());
    }

    @PartiallyConstant(n = 0, levels = Level.TRUTH, value = "((x|^-?\\d+$))*yz")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.LOW, value = "xyz")
    @Dynamic(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.HIGH, value = "(.*|.*yz)")
    public void ifElseInLoopWithAppendAfterwards() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            if (i % 2 == 0) {
                sb.append("x");
            } else {
                sb.append(i + 1);
            }
        }
        sb.append("yz");

        analyzeString(sb.toString());
    }

    @PartiallyConstant(n = 0, levels = Level.TRUTH, value = "a(b)*")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.LOW, value = "ab")
    @Dynamic(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.HIGH, value = ".*")
    public void nestedLoops(int range) {
        for (int i = 0; i < range; i++) {
            StringBuilder sb = new StringBuilder("a");
            for (int j = 0; j < range * range; j++) {
                sb.append("b");
            }
            analyzeString(sb.toString());
        }
    }

    @PartiallyConstant(n = 0, value = "((x|^-?\\d+$))*yz", levels = Level.TRUTH)
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.LOW, value = "xyz")
    @Dynamic(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.HIGH, value = "(.*|.*yz)")
    public void stringBufferExample() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 20; i++) {
            if (i % 2 == 0) {
                sb.append("x");
            } else {
                sb.append(i + 1);
            }
        }
        sb.append("yz");

        analyzeString(sb.toString());
    }

    @PartiallyConstant(n = 0, value = "a(b)*", levels = Level.TRUTH)
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.LOW, value = "ab")
    @Dynamic(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.HIGH, value = ".*")
    public void whileTrueWithBreak() {
        StringBuilder sb = new StringBuilder("a");
        while (true) {
            sb.append("b");
            if (sb.length() > 100) {
                break;
            }
        }
        analyzeString(sb.toString());
    }

    @PartiallyConstant(n = 0, value = "a(b)*", levels = Level.TRUTH)
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.LOW, value = "ab")
    @Dynamic(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.HIGH, value = ".*")
    public void whileNonTrueWithBreak(int i) {
        StringBuilder sb = new StringBuilder("a");
        int j = 0;
        while (j < i) {
            sb.append("b");
            if (sb.length() > 100) {
                break;
            }
            j++;
        }
        analyzeString(sb.toString());
    }

    @Constant(n = 0, levels = Level.TRUTH, value = "(iv1|iv2): ")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.LOW, value = "(iv1|iv2): ")
    // The real value is not fully resolved yet, since the string builder is used in a while loop,
    // which leads to the string builder potentially carrying any value. This can be refined by
    // recording pc specific states during data flow analysis.
    @Dynamic(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.HIGH, value = "((iv1|iv2): |.*)")
    @PartiallyConstant(n = 1, levels = Level.TRUTH, value = "(iv1|iv2): ((great!)?)*(java.lang.Runtime)?")
    @Failure(n = 1, levels = Level.L0)
    @Constant(n = 1, levels = Level.L1, soundness = SoundnessMode.LOW, value = "(iv1|iv2): great!")
    @Dynamic(n = 1, levels = Level.L1, soundness = SoundnessMode.HIGH, value = "(.*|.*.*)")
    @Constant(n = 1, levels = { Level.L2, Level.L3 }, soundness = SoundnessMode.LOW, value = "((iv1|iv2): great!|(iv1|iv2): great!java.lang.Runtime)")
    @Dynamic(n = 1, levels = { Level.L2, Level.L3 }, soundness = SoundnessMode.HIGH, value = "(.*|.*java.lang.Runtime)")
    public void extensiveWithManyControlStructures(boolean cond) {
        StringBuilder sb = new StringBuilder();
        if (cond) {
            sb.append("iv1");
        } else {
            sb.append("iv2");
        }
        System.out.println(sb);
        sb.append(": ");

        analyzeString(sb.toString());

        Random random = new Random();
        while (random.nextFloat() > 5.) {
            if (random.nextInt() % 2 == 0) {
                sb.append("great!");
            }
        }

        if (sb.indexOf("great!") > -1) {
            sb.append(getRuntimeClassName());
        }

        analyzeString(sb.toString());
    }

    // The bytecode produces an "if" within an "if" inside the first loop => two conditions
    @Constant(n = 0, levels = Level.TRUTH, value = "abc((d)?)*")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.LOW, value = "(abc|abcd)")
    @Dynamic(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.HIGH, value = ".*")
    @Constant(n = 1, levels = Level.TRUTH, value = "")
    @Failure(n = 1, levels = Level.L0)
    @Constant(n = 1, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.LOW, value = "")
    @Dynamic(n = 1, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.HIGH, value = "(|.*)")
    @Dynamic(n = 2, levels = Level.TRUTH, value = "((.*)?)*")
    @Failure(n = 2, levels = Level.L0)
    @Constant(n = 2, levels = Level.L1, soundness = SoundnessMode.LOW, value = "")
    @Constant(n = 2, levels = { Level.L2, Level.L3 }, soundness = SoundnessMode.LOW, value = "(|java.lang.Runtime)")
    @Dynamic(n = 2, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.HIGH, value = ".*")
    public void breakContinueExamples(int value) {
        StringBuilder sb1 = new StringBuilder("abc");
        for (int i = 0; i < value; i++) {
            if (i % 7 == 1) {
                break;
            } else if (i % 3 == 0) {
                continue;
            } else {
                sb1.append("d");
            }
        }
        analyzeString(sb1.toString());

        StringBuilder sb2 = new StringBuilder("");
        for (int i = 0; i < value; i++) {
            if (i % 2 == 0) {
                break;
            }
            sb2.append("some_value");
        }
        analyzeString(sb2.toString());

        StringBuilder sb3 = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (sb3.toString().equals("")) {
                // The analysis currently does not detect, that this statement is executed at
                // most / exactly once as it fully relies on the three-address code and does not
                // infer any semantics of conditionals
                sb3.append(getRuntimeClassName());
            } else {
                continue;
            }
        }
        analyzeString(sb3.toString());
    }

    /**
     * Some comprehensive example for experimental purposes taken from the JDK and slightly modified
     */
    @Constant(n = 0, levels = Level.TRUTH, value = "Hello: (java.lang.Runtime|java.lang.StringBuilder|StringBuilder)?")
    @Failure(n = 0, levels = Level.L0)
    @Constant(n = 0, levels = Level.L1, soundness = SoundnessMode.LOW, value = "Hello: ")
    @Constant(n = 0, levels = { Level.L2, Level.L3 }, soundness = SoundnessMode.LOW,
            value = "(Hello: |Hello: StringBuilder|Hello: java.lang.Runtime|Hello: java.lang.StringBuilder)")
    @Dynamic(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, soundness = SoundnessMode.HIGH, value = ".*")
    protected void setDebugFlags(String[] var1) {
        for(int var2 = 0; var2 < var1.length; ++var2) {
            String var3 = var1[var2];

            int randomValue = new Random().nextInt();
            StringBuilder sb = new StringBuilder("Hello: ");
            if (randomValue % 2 == 0) {
                sb.append(getRuntimeClassName());
            } else if (randomValue % 3 == 0) {
                sb.append(getStringBuilderClassName());
            } else if (randomValue % 4 == 0) {
                sb.append(getSimpleStringBuilderClassName());
            }

            try {
                Field var4 = this.getClass().getField(var3 + "DebugFlag");
                int var5 = var4.getModifiers();
                if (Modifier.isPublic(var5) && !Modifier.isStatic(var5) &&
                        var4.getType() == Boolean.TYPE) {
                    var4.setBoolean(this, true);
                }
            } catch (IndexOutOfBoundsException var90) {
                System.out.println("Should never happen!");
            } catch (Exception var6) {
                int i = 10;
                i += new Random().nextInt();
                System.out.println("Some severe error occurred!" + i);
            } finally {
                int i = 10;
                i += new Random().nextInt();
                if (i % 2 == 0) {
                    System.out.println("Ready to analyze now in any case!" + i);
                }
            }

            analyzeString(sb.toString());
        }
    }

    private String getRuntimeClassName() {
        return "java.lang.Runtime";
    }

    private String getStringBuilderClassName() {
        return "java.lang.StringBuilder";
    }

    private String getSimpleStringBuilderClassName() {
        return "StringBuilder";
    }
}
