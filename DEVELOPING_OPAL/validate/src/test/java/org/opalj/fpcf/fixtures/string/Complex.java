/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string;

import org.opalj.fpcf.properties.string_analysis.*;

import java.lang.reflect.Method;
import java.util.Random;

/**
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
    @Constant(n = 0, levels = Level.TRUTH, value = "Hello, World_paintname(_PAD|_REFLECT|_REPEAT)?(_AlphaTest)?")
    @Failure(n = 0, levels = Level.L0)
    // or-cases are currently not collapsed into simpler conditionals / or-cases using prefix checking
    @Constant(n = 0, levels = { Level.L1, Level.L2, Level.L3 }, value = "((Hello, World_paintname|Hello, World_paintname_PAD|Hello, World_paintname_REFLECT|Hello, World_paintname_REPEAT)_AlphaTest|Hello, World_paintname|Hello, World_paintname_PAD|Hello, World_paintname_REFLECT|Hello, World_paintname_REPEAT)")
    public void getPaintShader(boolean getPaintType, int spreadMethod, boolean alphaTest) {
        String shaderName = getHelloWorld() + "_" + "paintname";
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
