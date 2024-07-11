/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string_analysis.l1;

import org.opalj.fpcf.fixtures.string_analysis.l0.StringProvider;
import org.opalj.fpcf.fixtures.string_analysis.l1.hierarchies.GreetingService;
import org.opalj.fpcf.fixtures.string_analysis.l1.hierarchies.HelloGreeting;
import org.opalj.fpcf.fixtures.string_analysis.l0.L0TestMethods;
import org.opalj.fpcf.properties.string_analysis.*;

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
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "java.lang.Runtime")
            })
    public void simpleNonVirtualFunctionCallTest(int i) {
        analyzeString(getRuntimeClassName());
    }

    @StringDefinitionsCollection(
            value = "a case where a non-virtual function call inside an if statement is interpreted",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "(java.lang.Runtime|java.lang.StringBuilder|ERROR)")
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
            value = "a case where the initialization of a StringBuilder depends on > 1 non-virtual function calls and a constant",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "(java.lang.Runtime|java.lang.StringBuilder|ERROR)")
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

    @AllowedSoundnessModes(SoundnessMode.LOW)
    @StringDefinitionsCollection(
            value = "a case where a static method is called that returns a string but are not "
                    + "within this project => cannot / will not be interpret",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = INVALID, expectedStrings = StringDefinitions.INVALID_FLOW),
            })
    public void staticMethodOutOfScopeLowSoundnessTest() {
        analyzeString(System.clearProperty("os.version"));
    }

    @AllowedSoundnessModes(SoundnessMode.HIGH)
    @StringDefinitionsCollection(
            value = "a case where a static method is called that returns a string but are not "
                    + "within this project => cannot / will not be interpret",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = DYNAMIC, expectedStrings = ".*"),
            })
    public void staticMethodOutOfScopeTest() {
        analyzeString(System.clearProperty("os.version"));
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
                            expectedLevel = CONSTANT,
                            expectedStrings = "classname:StringBuilder,osname:someValue"
                    )
            })
    public void appendTest() {
        StringBuilder sb = new StringBuilder("classname:");
        sb.append(getSimpleStringBuilderClassName());
        sb.append(",osname:");
        sb.append(StringProvider.getSomeValue());
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

    @AllowedSoundnessModes(SoundnessMode.HIGH)
    @StringDefinitionsCollection(
            value = "a case taken from javax.management.remote.rmi.RMIConnector where a GetStatic is involved",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = PARTIALLY_CONSTANT, expectedStrings = ".*Impl_Stub")
            })
    public void getStaticTest() {
        analyzeString(rmiServerImplStubClassName);
    }

    @StringDefinitionsCollection(
            value = "a case where the append value has more than one def site with a function call involved",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "It is (great|Hello, World)")
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

    @AllowedSoundnessModes(SoundnessMode.HIGH)
    @StringDefinitionsCollection(
            value = "a case taken from com.sun.javafx.property.PropertyReference#reflect where "
                    + "a dependency within the finalize procedure is present",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT,
                            expectedStrings = "(get.*|getHello, Worldjava.lang.Runtime)"
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
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "(Hello, World|my.helper.Class)")
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

    public void belongsToSomeTestCaseAnotherTime() {
        callerWithFunctionParameterTest(belongsToTheSameTestCaseAnotherTime(), 900);
    }

    /**
     * Necessary for the callerWithFunctionParameterTest.
     */
    public static String belongsToTheSameTestCase() {
        return getHelloWorld();
    }
    public static String belongsToTheSameTestCaseAnotherTime() {
        return getHelperClass();
    }

    @StringDefinitionsCollection(
            value = "a function parameter being analyzed on its own",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "(Hello, World|my.helper.Class)")
            })
    public String callerWithFunctionParameterMultipleCallsInSameMethodTest(String s, float i) {
        analyzeString(s);
        return s;
    }

    /**
     * Necessary for the callerWithFunctionParameterMultipleCallsInSameMethodTest.
     */
    public void belongsToSomeTestCase3() {
        callerWithFunctionParameterMultipleCallsInSameMethodTest(belongsToTheSameTestCase(), 900);
        callerWithFunctionParameterMultipleCallsInSameMethodTest(belongsToTheSameTestCaseAnotherTime(), 900);
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
                            expectedStrings = "Hello, World_paintname(_PAD|_REFLECT|_REPEAT)?(_AlphaTest)?",
                            realisticLevel = CONSTANT,
                            // or-cases are currently not collapsed into simpler conditionals / or-cases using prefix checking
                            realisticStrings = "(Hello, World_paintname|Hello, World_paintname_PAD|Hello, World_paintname_REFLECT|Hello, World_paintname_REPEAT|(Hello, World_paintname|Hello, World_paintname_PAD|Hello, World_paintname_REFLECT|Hello, World_paintname_REPEAT)_AlphaTest)"
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
                            expectedStrings = "(another value|some value|^null$)",
                            // Contains a field write in the same method which cannot be captured by flow functions
                            realisticLevel = DYNAMIC,
                            realisticStrings = ".*"
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

    @AllowedSoundnessModes(SoundnessMode.LOW)
    @StringDefinitionsCollection(
            value = "a case where a field is read whose type is not supported",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = INVALID, expectedStrings = StringDefinitions.INVALID_FLOW)
            })
    public void nonSupportedFieldTypeReadLowSoundness() {
        analyzeString(myObject.toString());
    }

    @AllowedSoundnessModes(SoundnessMode.HIGH)
    @StringDefinitionsCollection(
            value = "a case where a field is read whose type is not supported",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = DYNAMIC, expectedStrings = ".*")
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

    // On L1, this test is equivalent to the `severalReturnValuesTest1`
    @AllowedDomainLevels(DomainLevel.L2)
    @StringDefinitionsCollection(
            value = "a case where the single valid return value of the called function can be resolved without calling the function",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "val",
                            // Since the virtual function return value is inlined in L2 and its actual runtime return
                            // value is not used, the function call gets converted to a method call, which modifies the
                            // TAC: The def PC from the `analyzeString` parameter is now different and points to the def
                            // PC for the `resolvableReturnValueFunction` parameter. This results in no string flow being
                            // detected since the def and use sites are now inconsistent.
                            realisticLevel = INVALID, realisticStrings = StringDefinitions.INVALID_FLOW
                    )
            })
    public void resolvableReturnValue() {
        analyzeString(resolvableReturnValueFunction("val", 42));
    }

    /**
     * Belongs to resolvableReturnValue.
     */
    private String resolvableReturnValueFunction(String s, int i) {
        switch (i) {
            case 0: return getObjectClassName();
            case 1: return "One";
            default: return s;
        }
    }

    @StringDefinitionsCollection(
            value = "a case where a non virtual function has multiple return values",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "(One|val|java.lang.Object)")
            })
    public void severalReturnValuesTest1() {
        analyzeString(severalReturnValuesFunction("val", 42));
    }

    /**
     * Belongs to severalReturnValuesTest1.
     */
    private String severalReturnValuesFunction(String s, int i) {
        switch (i) {
            case 0: return "One";
            case 1: return s;
            default: return getObjectClassName();
        }
    }

    @StringDefinitionsCollection(
            value = "a case where a static function has multiple return values",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "(that's odd|my.helper.Class)")
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
                    @StringDefinitions(expectedLevel = INVALID, expectedStrings = StringDefinitions.INVALID_FLOW)
            })
    public void functionWithNoReturnValueTest1() {
        analyzeString(noReturnFunction1());
    }

    /**
     * Belongs to functionWithNoReturnValueTest1.
     */
    public String noReturnFunction1() {
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

    @AllowedSoundnessModes(SoundnessMode.HIGH)
    @StringDefinitionsCollection(
            value = "a case where a String array field is read",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "(January|February|March|April)",
                            realisticLevel = DYNAMIC,
                            realisticStrings = ".*"
                    )
            })
    public void getStringArrayField(int i) {
        analyzeString(monthNames[i]);
    }

    // DIFFERING TEST CASES FROM PREVIOUS LEVELS

    @StringDefinitionsCollection(
            value = "a more comprehensive case where multiple definition sites have to be "
                    + "considered each with a different string generation mechanism",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "((java.lang.Object|java.lang.Runtime)|java.lang.System|java.lang.StringBuilder)",
                            realisticLevel = DYNAMIC,
                            // Array values are currently not interpreted. Also, differently constructed strings are
                            // currently not deduplicated since they result in different string trees during flow analysis.
                            realisticStrings = "(.*|java.lang.System|java.lang.StringBuilder|java.lang.StringBuilder)"
                    )
            })
    public void multipleDefSites(int value) {}

    @StringDefinitionsCollection(
            value = "a test case which tests the interpretation of String#valueOf",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "c"),
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "42.3"),
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "java.lang.Runtime")
            })
    public void valueOfTest() {}

    @StringDefinitionsCollection(
            value = "an example that uses a non final field",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "Field Value:private l0 non-final string field")
            })
    public void nonFinalFieldRead() {}

    @StringDefinitionsCollection(
            value = "can handle virtual function calls",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "java.lang.StringBuilder")
            })
    public void fromFunctionCallLowSoundness() {}

    @StringDefinitionsCollection(
            value = "can handle virtual function calls",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "java.lang.StringBuilder")
            })
    public void fromFunctionCall() {}

    @StringDefinitionsCollection(
            value = "constant string + string from function call => CONSTANT",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "java.lang.StringBuilder")
            })
    public void fromConstantAndFunctionCallLowSoundness() {}

    @StringDefinitionsCollection(
            value = "constant string + string from function call => CONSTANT",
            stringDefinitions = {
                    @StringDefinitions(expectedLevel = CONSTANT, expectedStrings = "java.lang.StringBuilder")
            })
    public void fromConstantAndFunctionCall() {}

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
    protected void setDebugFlags(String[] var1) {}

    @StringDefinitionsCollection(
            value = "an extensive example with many control structures",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT, expectedStrings = "(iv1|iv2): ",
                            // The real value is not fully resolved yet, since the string builder is used in a while loop,
                            // which leads to the string builder potentially carrying any value. This can be refined by
                            // recording pc specific states during data flow analysis.
                            realisticLevel = DYNAMIC, realisticStrings = "((iv1|iv2): |.*)"
                    ),
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "(iv1|iv2): ((great!)?)*(java.lang.Runtime)?",
                            realisticLevel = DYNAMIC,
                            realisticStrings = "(.*|.*java.lang.Runtime)"
                    )
            })
    public void extensive(boolean cond) {}

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
