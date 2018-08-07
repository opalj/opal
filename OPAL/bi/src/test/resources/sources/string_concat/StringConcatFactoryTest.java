/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package string_concat;

public class StringConcatFactoryTest {

    public static String simpleConcat(String s1, String s2){
        return s1 + s2;
    }

    public static String concatConstants(String s1, String s2){
        return s1 + " " + s2 + 5;
    }

    public static String concatObjectAndInt(String s, Object o, int i){
        return s + o + i;
    }

    public static String concatObjectAndDoubleWithConstants(Object o, double d){
        return " " + d + 2.5 + o;
    }

    public static String concatLongAndConstant(long l, String s){
        return s + 15L + l;
    }

    public static String concatClassConstant(String s){
        return s + StringConcatFactoryTest.class;
    }

    public static String concatNonInlineableConstant(String s){
        return s + "\u0001\u0002";
    }
}
