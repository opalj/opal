/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string;

import org.opalj.fpcf.properties.string_analysis.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.Scanner;

/**
 * Various tests that test certain complex string analysis scenarios which were either constructed or extracted from the
 * JDK. Such tests should combine multiple string analysis techniques, the most common are interprocedurality and
 * control flow sensitivity.
 *
 * @see SimpleStringOps
 */
public class Complex {

    /**
     * Serves as the sink for string variables to be analyzed.
     */
    public void analyzeString(String s) {}

    /**
     * Taken from com.sun.javafx.property.PropertyReference#reflect.
     */
    @Constant(n = 0, levels = Level.TRUTH, soundness = SoundnessMode.LOW, value = "get-Hello, World-java.lang.Runtime")
    @PartiallyConstant(n = 0, levels = Level.TRUTH, soundness = SoundnessMode.HIGH, value = "(get-.*|get-Hello, World-java.lang.Runtime)")
    @Failure(n = 0, levels = Level.L0)
    @Invalid(n = 0, levels = Level.L1, soundness = SoundnessMode.LOW)
    @PartiallyConstant(n = 0, levels = Level.L1, soundness = SoundnessMode.HIGH, value = "(get-.*|get-Hello, World-.*)")
    public void complexDependencyResolve(String s, Class clazz) {
        String properName = s.length() == 1 ? s.substring(0, 1) :
                getHelloWorld() + "-" + getRuntimeClassName();
        String getterName = "get-" + properName;
        Method m;
        try {
            m = clazz.getMethod(getterName);
            System.out.println(m);
            analyzeString(getterName);
        } catch (NoSuchMethodException var13) {
        }
    }

    /**
     * Taken from com.sun.prism.impl.ps.BaseShaderContext#getPaintShader and slightly adapted
     */
    @Constant(n = 0, levels = Level.TRUTH, value = "Hello, World_paintName(_PAD|_REFLECT|_REPEAT)?(_AlphaTest)?")
    @Failure(n = 0, levels = Level.L0)
    // or-cases are currently not collapsed into simpler conditionals / or-cases using prefix checking
    @Constant(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, value = "((Hello, World_paintName|Hello, World_paintName_PAD|Hello, World_paintName_REFLECT|Hello, World_paintName_REPEAT)_AlphaTest|Hello, World_paintName|Hello, World_paintName_PAD|Hello, World_paintName_REFLECT|Hello, World_paintName_REPEAT)")
    public void getPaintShader(boolean getPaintType, int spreadMethod, boolean alphaTest) {
        String shaderName = getHelloWorld() + "_" + "paintName";
        if (getPaintType) {
            if (spreadMethod == 0) {
                shaderName = shaderName + "_PAD";
            } else if (spreadMethod == 1) {
                shaderName = shaderName + "_REFLECT";
            } else if (spreadMethod == 2) {
                shaderName = shaderName + "_REPEAT";
            }
        }
        if (alphaTest) {
            shaderName = shaderName + "_AlphaTest";
        }
        analyzeString(shaderName);
    }

    @Failure(n = 0, levels = Level.TRUTH)
    @Failure(n = 1, levels = Level.TRUTH)
    public void unknownCharValue() {
        int charCode = new Random().nextInt(200);
        char c = (char) charCode;
        String s = String.valueOf(c);
        analyzeString(s);

        StringBuilder sb = new StringBuilder();
        sb.append(c);
        analyzeString(sb.toString());
    }

    @Failure(n = 0, levels = { Level.L0, Level.L1 })
    @Constant(n = 0, levels = { Level.L2, Level.L3 }, value = "value")
    public String cyclicDependencyTest(String s) {
        String value = getProperty(s);
        analyzeString(value);
        return value;
    }

    /**
     * Methods are called that return a string but are not within this project => cannot / will not interpret
     */
    @Dynamic(n = 0, levels = Level.TRUTH, value = "(.*)*")
    @Failure(n = 0, levels = { Level.L0, Level.L1, Level.L2, Level.L3 })
    @Invalid(n = 1, levels = Level.TRUTH, soundness = SoundnessMode.LOW)
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

    private String getProperty(String name) {
        if (name == null) {
            return cyclicDependencyTest("default");
        } else {
            return "value";
        }
    }

    private String getRuntimeClassName() {
        return "java.lang.Runtime";
    }

    private static String getHelloWorld() {
        return "Hello, World";
    }
}
