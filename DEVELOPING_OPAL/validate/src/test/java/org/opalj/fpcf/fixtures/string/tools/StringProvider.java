/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string.tools;

public class StringProvider {

    /**
     * Returns "[packageName].[className]".
     */
    public static String concat(String firstString, String secondString) {
        return firstString + secondString;
    }

    /**
     * Returns "[packageName].[className]".
     */
    public static String getFQClassNameWithStringBuilder(String packageName, String className) {
        return (new StringBuilder()).append(packageName).append(".").append(className).toString();
    }

    public static String getSomeValue() {
        return "someValue";
    }
}
