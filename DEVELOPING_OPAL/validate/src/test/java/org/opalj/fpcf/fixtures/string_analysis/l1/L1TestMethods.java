/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string_analysis.l1;

import org.opalj.fpcf.fixtures.string_analysis.l1.hierarchies.GreetingService;
import org.opalj.fpcf.fixtures.string_analysis.l1.hierarchies.HelloGreeting;
import org.opalj.fpcf.fixtures.string_analysis.l0.L0TestMethods;
import org.opalj.fpcf.properties.string_analysis.StringDefinitions;
import org.opalj.fpcf.properties.string_analysis.StringDefinitionsCollection;

import javax.management.remote.rmi.RMIServer;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Random;
import java.util.Scanner;

import static org.opalj.fpcf.properties.string_analysis.StringConstancyLevel.*;

/**
 * This file contains various tests for the L1StringAnalysis. For further information on what to consider, please see
 * {@link L0TestMethods}.
 *
 * @author Patrick Mell
 */
public class L1TestMethods extends L0TestMethods {

    public static final String JAVA_LANG = "java.lang";
    private static final String rmiServerImplStubClassName =
            RMIServer.class.getName() + "Impl_Stub";

    private String myField;

    private String noWriteField;

    private Object myObject;

    private String fieldWithInit = "init field value";

    private String fieldWithConstructorInit;

    private float secretNumber;

    public static String someKey = "will not be revealed here";

    private String[] monthNames = { "January", "February", "March", getApril() };

    /**
     * {@see L0TestMethods#analyzeString}
     */
    public void analyzeString(String s) {
    }

    public L1TestMethods(float e) {
        fieldWithConstructorInit = "initialized by constructor";
        secretNumber = e;
    }

    @StringDefinitionsCollection(
            value = "a case where a very simple non-virtual function call is interpreted",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "(java.lang.Runtime|java.lang.StringBuilder|ERROR)"
                    )
            })
    public void simpleNonVirtualFunctionCallTest(int i) {
        analyzeString(getRuntimeClassName());
    }

    @StringDefinitionsCollection(
            value = "a case where a non-virtual function call inside an if statement is interpreted",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "(java.lang.Runtime|java.lang.StringBuilder|ERROR)",
                            realisticLevel = DYNAMIC,
                            realisticStrings = ".*"
                    )
            })
    public void simpleNonVirtualFunctionCallTestWithIf(int i) {
        String s;
        if (i == 0) {
            s = getRuntimeClassName();
        } else if (i == 1) {
            s = getStringBuilderClassName();
        } else {
            s = "ERROR";
        }
        analyzeString(s);
    }

    @StringDefinitionsCollection(
            value = "a case where the initialization of a StringBuilder depends on > 1 non-virtual "
                    + "function calls and a constant",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "(java.lang.Runtime|java.lang.StringBuilder|ERROR)",
                            realisticLevel = DYNAMIC,
                            realisticStrings = ".*"
                    )
            })
    public void initFromNonVirtualFunctionCallTest(int i) {
        String s;
        if (i == 0) {
            s = getRuntimeClassName();
        } else if (i == 1) {
            s = getStringBuilderClassName();
        } else {
            s = "ERROR";
        }
        StringBuilder sb = new StringBuilder(s);
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "a case where a static method is called that returns a string but are not "
                    + "within this project => cannot / will not be interpret",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = DYNAMIC, expectedStrings = ".*"),
            })
    public void staticMethodOutOfScopeTest() {
        analyzeString(System.getProperty("os.version"));
    }

    @StringDefinitionsCollection(
            value = "a case where a (virtual) method is called that return a string but are not "
                    + "within this project => cannot / will not interpret",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = DYNAMIC,
                            expectedStrings = "(.*)*",
                            realisticLevel = DYNAMIC,
                            realisticStrings = ".*"
                    )
            })
    public void methodOutOfScopeTest() throws FileNotFoundException {
        File file = new File("my-file.txt");
        Scanner sc = new Scanner(file);
        StringBuilder sb = new StringBuilder();
        while (sc.hasNextLine()) {
            sb.append(sc.nextLine());
        }
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "a case where function calls are involved in append operations",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT,
                            expectedStrings = "classname:StringBuilder,osname:.*"
                    )
            })
    public void appendTest() {
        StringBuilder sb = new StringBuilder("classname:");
        sb.append(getSimpleStringBuilderClassName());
        sb.append(",osname:");
        sb.append(System.getProperty("os.name:"));
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "a case where the concrete instance of an interface is known",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "Hello World")
            })
    public void knownHierarchyInstanceTest() {
        GreetingService gs = new HelloGreeting();
        analyzeString(gs.getGreeting("World"));
    }

    @StringDefinitionsCollection(
            value = "a case where the concrete instance of an interface is NOT known",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "(Hello World|Hello)")
            })
    public void unknownHierarchyInstanceTest(GreetingService greetingService) {
        analyzeString(greetingService.getGreeting("World"));
    }

    @StringDefinitionsCollection(
            value = "a case taken from javax.management.remote.rmi.RMIConnector where a GetStatic is involved",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = PARTIALLY_CONSTANT, expectedStrings = ".*Impl_Stub")
            })
    public void getStaticTest() {
        analyzeString(rmiServerImplStubClassName);
    }

    @StringDefinitionsCollection(
            value = "a case where the append value has more than one def site",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "It is (great|not great)",
                            realisticLevel = DYNAMIC, realisticStrings = ".*"
                    )
            })
    public void appendWithTwoDefSites(int i) {
        String s;
        if (i > 0) {
            s = "great";
        } else {
            s = "not great";
        }
        analyzeString(new StringBuilder("It is ").append(s).toString());
    }

    @StringDefinitionsCollection(
            value = "a case where the append value has more than one def site with a function call involved",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "It is (great|Hello, World)",
                            realisticLevel = DYNAMIC, realisticStrings = ".*"
                    )
            })
    public void appendWithTwoDefSitesWithFuncCallTest(int i) {
        String s;
        if (i > 0) {
            s = "great";
        } else {
            s = getHelloWorld();
        }
        analyzeString(new StringBuilder("It is ").append(s).toString());
    }

    @StringDefinitionsCollection(
            value = "a case taken from com.sun.javafx.property.PropertyReference#reflect where "
                    + "a dependency within the finalize procedure is present",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT,
                            expectedStrings = "get(.*|Hello, Worldjava.lang.Runtime)",
                            realisticLevel = DYNAMIC,
                            realisticStrings = ".*"
                    )
            })
    public void dependenciesWithinFinalizeTest(String s, Class clazz) {
        String properName = s.length() == 1 ? s.substring(0, 1).toUpperCase() :
                getHelloWorld() + getRuntimeClassName();
        String getterName = "get" + properName;
        Method m;
        try {
            m = clazz.getMethod(getterName);
            System.out.println(m);
            analyzeString(getterName);
        } catch (NoSuchMethodException var13) {
        }
    }

    @StringDefinitionsCollection(
            value = "a function parameter being analyzed on its own",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = DYNAMIC, expectedStrings = ".*")
            })
    public String callerWithFunctionParameterTest(String s, float i) {
        analyzeString(s);
        return s;
    }

    @StringDefinitionsCollection(
            value = "a case taken from javax.management.remote.rmi.RMIConnector where a GetStatic is involved",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "Hello, World")
            })
    public void belongsToSomeTestCase() {
        String s = callerWithFunctionParameterTest(belongsToTheSameTestCase(), 900);
        analyzeString(s);
    }

    /**
     * Necessary for the callerWithFunctionParameterTest.
     */
    public static String belongsToTheSameTestCase() {
        return getHelloWorld();
    }

    @StringDefinitionsCollection(
            value = "a case where a function takes another function as one of its parameters",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "Hello, World!"),
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "Hello, World?")
            })
    public void functionWithFunctionParameter() {
        analyzeString(addExclamationMark(getHelloWorld()));
        analyzeString(addQuestionMark(getHelloWorld()));
    }

    @StringDefinitionsCollection(
            value = "a case where no callers information need to be computed",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "java.lang.String")
            })
    public void noCallersInformationRequiredTest(String s) {
        System.out.println(s);
        analyzeString("java.lang.String");
    }

    @StringDefinitionsCollection(
            value = "a case taken from com.sun.prism.impl.ps.BaseShaderContext#getPaintShader and slightly adapted",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "Hello, World_paintname((_PAD|_REFLECT|_REPEAT)?)?(_AlphaTest)?",
                            realisticLevel = DYNAMIC,
                            realisticStrings = ".*"
                    )
            })
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

    /**
     * Necessary for the tieName test.
     */
    private static String tieNameForCompiler(String var0) {
        return var0 + "_tie";
    }

    @StringDefinitionsCollection(
            value = "a case where a string field is read",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "(another value|some value|^null$)"
                    )
            })
    public void fieldReadTest() {
        myField = "some value";
        analyzeString(myField);
    }

    private void belongsToFieldReadTest() {
        myField = "another value";
    }

    @StringDefinitionsCollection(
            value = "a case where a field is read which is not written",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = DYNAMIC,
                            expectedStrings = "(^null$|.*)"
                    )
            })
    public void fieldWithNoWriteTest() {
        analyzeString(noWriteField);
    }

    @StringDefinitionsCollection(
            value = "a case where a field is read whose type is not supported",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = DYNAMIC,
                            expectedStrings = ".*"
                    )
            })
    public void nonSupportedFieldTypeRead() {
        analyzeString(myObject.toString());
    }

    @StringDefinitionsCollection(
            value = "a case where a field is declared and initialized",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "init field value"
                    )
            })
    public void fieldWithInitTest() {
        analyzeString(fieldWithInit.toString());
    }

    @StringDefinitionsCollection(
            value = "a case where a field is initialized in a constructor",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "initialized by constructor"
                    )
            })
    public void fieldInitByConstructor() {
        analyzeString(fieldWithConstructorInit.toString());
    }

    @StringDefinitionsCollection(
            value = "a case where a field is initialized with a value of a constructor parameter",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = DYNAMIC,
                            expectedStrings = "^-?\\d*\\.{0,1}\\d+$"
                    )
            })
    public void fieldInitByConstructorParameter() {
        analyzeString(new StringBuilder().append(secretNumber).toString());
    }

    @StringDefinitionsCollection(
            value = "a case where no callers information need to be computed",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = DYNAMIC,
                            expectedStrings = "(.*|value)"
                    )
            })
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

    @StringDefinitionsCollection(
            value = "a case where a non virtual function has multiple return values",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "(One|val|java.lang.Object)"
                    )
            })
    public void severalReturnValuesTest1() {
        analyzeString(severalReturnValuesFunction("val", 42));
    }

    /**
     * Belongs to severalReturnValuesTest1.
     */
    private String severalReturnValuesFunction(String s, int i) {
        switch (i) {
        case 0: return getObjectClassName();
        case 1: return "One";
        default: return s;
        }
    }

    @StringDefinitionsCollection(
            value = "a case where a static function has multiple return values",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "(that's odd|my.helper.Class)"
                    )
            })
    public void severalReturnValuesTest2() {
        analyzeString(severalReturnValuesStaticFunction(42));
    }

    /**
     * Belongs to severalReturnValuesTest2.
     */
    private static String severalReturnValuesStaticFunction(int i) {
        // The ternary operator would create only a single "return" statement which is not what we
        // want here
        if (i % 2 != 0) {
            return "that's odd";
        } else {
            return getHelperClass();
        }
    }

    @StringDefinitionsCollection(
            value = "a case where a non-virtual and a static function have no return values at all",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = DYNAMIC,
                            expectedStrings = ".*"
                    ),
                    @StringDefinitions(
                            expectedLevel = DYNAMIC,
                            expectedStrings = ".*"
                    )
            })
    public void functionWithNoReturnValueTest1() {
        analyzeString(noReturnFunction1());
        analyzeString(noReturnFunction2());
    }

    /**
     * Belongs to functionWithNoReturnValueTest1.
     */
    public String noReturnFunction1() {
        throw new RuntimeException();
    }

    /**
     * Belongs to functionWithNoReturnValueTest1.
     */
    public static String noReturnFunction2() {
        throw new RuntimeException();
    }

    @StringDefinitionsCollection(
            value = "a case where a static property is read",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "will not be revealed here")
            })
    public void getStaticFieldTest() {
        analyzeString(someKey);
    }

    @StringDefinitionsCollection(
            value = "a case where a String array field is read",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "(January|February|March|April)")
            })
    public void getStringArrayField(int i) {
        analyzeString(monthNames[i]);
    }

    // DIFFERING TEST CASES FROM PREVIOUS LEVELS

    @StringDefinitionsCollection(
            value = "a test case which tests the interpretation of String#valueOf",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "c"),
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "42.3"),
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "java.lang.Runtime")
            })
    public void valueOfTest() {
        analyzeString(String.valueOf('c'));
        analyzeString(String.valueOf((float) 42.3));
        analyzeString(String.valueOf(getRuntimeClassName()));
    }

    @StringDefinitionsCollection(
            value = "can handle virtual function calls",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "java.lang.StringBuilder")
            })
    public void fromFunctionCall() {
        String className = getStringBuilderClassName();
        analyzeString(className);
    }

    @StringDefinitionsCollection(
            value = "Some comprehensive example for experimental purposes taken from the JDK and slightly modified",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "Hello: (java.lang.Runtime|java.lang.StringBuilder|StringBuilder)?",
                            realisticLevel = DYNAMIC,
                            realisticStrings = ".*"
                    ),
            })
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
                // TODO: Control structures in finally blocks are not handles correctly
                // if (i % 2 == 0) {
                //     System.out.println("Ready to analyze now in any case!" + i);
                // }
            }

            analyzeString(sb.toString());
        }
    }

    @StringDefinitionsCollection(
            value = "an extensive example with many control structures",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "(iv1|iv2): ",
                            realisticLevel = DYNAMIC, realisticStrings = ".*"
                    ),
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "(iv1|iv2): ((great!)?)*(java.lang.Runtime)?",
                            realisticLevel = DYNAMIC,
                            realisticStrings = ".*"
                    )
            })
    public void extensive(boolean cond) {
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

    private String getRuntimeClassName() {
        return "java.lang.Runtime";
    }

    private String getStringBuilderClassName() {
        return "java.lang.StringBuilder";
    }

    private String getSimpleStringBuilderClassName() {
        return "StringBuilder";
    }

    private static String getHelloWorld() {
        return "Hello, World";
    }

    private static String addExclamationMark(String s) {
        return s + "!";
    }

    private String addQuestionMark(String s) {
        return s + "?";
    }

    private String getObjectClassName() {
        return "java.lang.Object";
    }

    private static String getHelperClass() {
        return "my.helper.Class";
    }

    private String getApril() {
        return "April";
    }
}
