/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string_analysis;

import com.sun.corba.se.impl.util.PackagePrefixChecker;
import org.opalj.fpcf.fixtures.string_analysis.hierarchies.GreetingService;
import org.opalj.fpcf.fixtures.string_analysis.hierarchies.HelloGreeting;
import org.opalj.fpcf.properties.string_analysis.StringDefinitions;
import org.opalj.fpcf.properties.string_analysis.StringDefinitionsCollection;

import javax.management.remote.rmi.RMIServer;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.util.Scanner;

import static org.opalj.fpcf.properties.string_analysis.StringConstancyLevel.*;

/**
 * This file contains various tests for the InterproceduralStringAnalysis. For further information
 * on what to consider, please see {@link LocalTestMethods}
 *
 * @author Patrick Mell
 */
public class InterproceduralTestMethods {

    public static final String JAVA_LANG = "java.lang";
    private static final String rmiServerImplStubClassName =
            RMIServer.class.getName() + "Impl_Stub";

    private String myField;

    private String noWriteField;

    /**
     * {@see LocalTestMethods#analyzeString}
     */
    public void analyzeString(String s) {
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
                            expectedStrings = "(java.lang.Runtime|java.lang.StringBuilder|ERROR)"
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
            value = "a case where a static method with a string parameter is called",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "java.lang.(Integer|Object|Runtime)"
                    )
            })
    public void fromStaticMethodWithParamTest() {
        analyzeString(StringProvider.getFQClassName(JAVA_LANG, "Integer"));
    }

    @StringDefinitionsCollection(
            value = "a case where a static method is called that returns a string but are not "
                    + "within this project => cannot / will not be interpret",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = DYNAMIC,
                            expectedStrings = "\\w"
                    ),

            })
    public void staticMethodOutOfScopeTest() throws FileNotFoundException {
        analyzeString(System.getProperty("os.version"));
    }

    @StringDefinitionsCollection(
            value = "a case where a (virtual) method is called that return a string but are not "
                    + "within this project => cannot / will not interpret",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = DYNAMIC,
                            expectedStrings = "(\\w)*"
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
            value = "a case where an array access needs to be interpreted interprocedurally",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = DYNAMIC,
                            expectedStrings = "(java.lang.Object|\\w|java.lang.(Integer|"
                                    + "Object|Runtime)|\\w)"
                    )

            })
    public void arrayTest(int i) {
        String[] classes = {
                "java.lang.Object",
                getRuntimeClassName(),
                StringProvider.getFQClassName("java.lang", "Integer"),
                System.getProperty("SomeClass")
        };
        analyzeString(classes[i]);
    }

    @StringDefinitionsCollection(
            value = "a case that tests that the append interpretation of only intraprocedural "
                    + "expressions still works",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "value:(A|BC)Z"
                    )

            })
    public void appendTest0(int i) {
        StringBuilder sb = new StringBuilder("value:");
        if (i % 2 == 0) {
            sb.append('A');
        } else {
            sb.append("BC");
        }
        sb.append('Z');
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "a case where function calls are involved in append operations",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT,
                            expectedStrings = "classname:StringBuilder,osname:\\w"
                    )

            })
    public void appendTest1() {
        StringBuilder sb = new StringBuilder("classname:");
        sb.append(getSimpleStringBuilderClassName());
        sb.append(",osname:");
        sb.append(System.getProperty("os.name:"));
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "a case where function calls are involved in append operations",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "(java.lang.Runtime|java.lang.StringBuilder|"
                                    + "ERROR!) - Done"
                    )

            })
    public void appendTest2(int classToLoad) {
        StringBuilder sb;
        if (classToLoad == 0) {
            sb = new StringBuilder(getRuntimeClassName());
        } else if (classToLoad == 1) {
            sb = new StringBuilder(getStringBuilderClassName());
        } else {
            sb = new StringBuilder("ERROR!");
        }
        sb.append(" - Done");
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "a case where the concrete instance of an interface is known",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "Hello World"
                    )

            })
    public void knownHierarchyInstanceTest() {
        GreetingService gs = new HelloGreeting();
        analyzeString(gs.getGreeting("World"));
    }

    @StringDefinitionsCollection(
            value = "a case where the concrete instance of an interface is NOT known",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "(Hello World|Hello)"
                    )

            })
    public void unknownHierarchyInstanceTest(GreetingService greetingService) {
        analyzeString(greetingService.getGreeting("World"));
    }

    @StringDefinitionsCollection(
            value = "a case where context-insensitivity is tested",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "java.lang.(Integer|Object|Runtime)"
                    )

            })
    public void contextInsensitivityTest() {
        StringBuilder sb = new StringBuilder();
        String s = StringProvider.getFQClassName("java.lang", "Object");
        sb.append(StringProvider.getFQClassName("java.lang", "Runtime"));
        analyzeString(sb.toString());
    }

    @StringDefinitionsCollection(
            value = "a case taken from javax.management.remote.rmi.RMIConnector where a GetStatic "
                    + "is involved",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = DYNAMIC,
                            expectedStrings = "\\w"
                    )

            })
    public void getStaticTest() {
        analyzeString(rmiServerImplStubClassName);
    }

    @StringDefinitionsCollection(
            value = "a case where the append value has more than one def site",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "It is (great|not great)"
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
            value = "a case where the append value has more than one def site with a function "
                    + "call involved",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "It is (great|Hello, World)"
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
                            expectedStrings = "get(\\w|Hello, Worldjava.lang.Runtime)"
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
            value = "a case taken from javax.management.remote.rmi.RMIConnector where a GetStatic "
                    + "is involved",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "Hello, World"
                    )

            })
    public String callerWithFunctionParameterTest(String s, float i) {
        analyzeString(s);
        return s;
    }

    /**
     * Necessary for the callerWithFunctionParameterTest.
     */
    public void belongsToSomeTestCase() {
        String s = callerWithFunctionParameterTest(belongsToTheSameTestCase(), 900);
        System.out.println(s);
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
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "Hello, World!"
                    ),
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "Hello, World?"
                    )
            })
    public void functionWithFunctionParameter() {
        analyzeString(addExclamationMark(getHelloWorld()));
        analyzeString(addQuestionMark(getHelloWorld()));
    }

    @StringDefinitionsCollection(
            value = "a case where no callers information need to be computed",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "java.lang.String"
                    )
            })
    public void noCallersInformationRequiredTest(String s) {
        System.out.println(s);
        analyzeString("java.lang.String");
    }

    @StringDefinitionsCollection(
            value = "a case taken from com.sun.prism.impl.ps.BaseShaderContext#getPaintShader "
                    + "and slightly adapted",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = CONSTANT,
                            expectedStrings = "Hello, World_paintname((_PAD|_REFLECT|_REPEAT)?)?"
                                    + "(_AlphaTest)?"
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

    @StringDefinitionsCollection(
            value = "a case taken from com.sun.corba.se.impl.util.Utility#tieName and slightly "
                    + "adapted",
            stringDefinitions = {
                    @StringDefinitions(
                            expectedLevel = PARTIALLY_CONSTANT,
                            expectedStrings = "(\\w\\w_tie|\\w_tie)"
                    )
            })
    public String tieName(String var0, Remote remote) {
        PackagePrefixChecker ppc = new PackagePrefixChecker();
        String s = PackagePrefixChecker.hasOffendingPrefix(tieNameForCompiler(var0)) ?
                PackagePrefixChecker.packagePrefix() + tieNameForCompiler(var0) :
                tieNameForCompiler(var0);
        analyzeString(s);
        return s;
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
                            expectedStrings = "(some value|another value)"
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
                            expectedStrings = "(^null$|\\w)"
                    )
            })
    public void fieldWithNoWriteTest() {
        analyzeString(noWriteField);
    }

//    @StringDefinitionsCollection(
//            value = "a case where no callers information need to be computed",
//            stringDefinitions = {
//                    @StringDefinitions(
//                            expectedLevel = CONSTANT,
//                            expectedStrings = "value"
//                    )
//            })
//    public String cyclicDependencyTest(String s) {
//        String value = getProperty(s);
//        analyzeString(value);
//        return value;
//    }
//
//    private String getProperty(String name) {
//        if (name == null) {
//            return cyclicDependencyTest("default");
//        } else {
//            return "value";
//        }
//    }

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

}
