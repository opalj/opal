/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Leonid Glanz
 */
public class Obfuscation {

    public static final void dead() {
        throw new UnsupportedOperationException("Wrong Test");
    }

    public static void main(String[] args) {
        emptyStringArray();
        filledStringArray();
        filledArrayCollection();
        arrayCopy();
        arrayToString();
        deadArrayLength(100);
        hashCodeDead();
        equalsDead();
        equalsDead2();
        arraysUseDead();
    }

    public static void emptyStringArray() {
        String str = "";
        if (str.toCharArray().length > 0) {
            dead();
        } else {
            str = "hello";
        }
    }

    public static void filledStringArray() {
        String str = "hello";
        if (str.toCharArray().length == 0) {
            dead();
        } else {
            str = "";
        }
    }

    public static void filledArrayCollection() {
        String[] strs = new String[] {};
        List<String> list = new ArrayList<String>();

        list.add("hello");
        list.add("world");
        list.add("nice");
        list.add("day");

        Object[] obs = list.toArray(strs);
        if (obs.length == 0) {
            dead();
        }
    }

    public static void arrayCopy() {
        String str = "hello world";
        char[] chars = new char[0];
        System.arraycopy(str.toCharArray(), 5, chars, 0, 0);
        if (chars.length > 0) {
            dead();
        }
    }

    public static void arrayToString() {
        String str = "hello world, I hope you have a nice day";
        String str2 = str.toCharArray().toString();
        if (str2.length() > 20) {
            dead();
        }
    }

    public static void deadArrayLength(int n) {
        int len = (n * n + n) % 2 == 0 ? 0 : 7;
        int[] ints = new int[len];
        if (ints.length > 0) {
            dead();
        }
    }

    public static void hashCodeDead() {
        String str = "hello world";
        char[] chs = str.toCharArray();
        if (chs.hashCode() == str.hashCode()) {
            dead();
        }
    }

    public static void equalsDead() {
        char[] chs = "hello".toCharArray();
        char[] chs3 = new char[] { 'h', 'e', 'l', 'l', 'o' };
        if (chs.equals(chs3)) {
            dead();
        }
    }

    public static void equalsDead2() {
        char[] chs = "hello".toCharArray();
        Character h = new Character('h');
        Character e = new Character('e');
        Character l = new Character('l');
        Character o = new Character('o');
        char[] chs2 = new char[] { h, e, l, l, o };
        if (chs.equals(chs2)) {
            dead();
        }
    }

    public static void arraysUseDead() {
        if (Arrays.toString(new char[] { 'h', 'e', 'l', 'l', 'o' }).length() == 5) {
            dead();
        }
    }

}
