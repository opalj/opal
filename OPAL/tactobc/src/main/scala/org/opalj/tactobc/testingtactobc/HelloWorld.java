package org.opalj.tactobc.testingtactobc;

public class HelloWorld {
    public static void main(String[] args) {
        //todo: separate cases in methods to have testscenarios
//        double[][] test1 = new double[1][3];
//        test1[0][0] = 1.1d;
//        System.out.println(test1[0][0] * 2);
//        String[] strArray = new String[3];
        double d = 1.1d;
        System.out.println(d);
//        String[] str = new String[2];
//        str[0] = "Hallo";
//        System.out.println(str[0]);
//        double[] yay = new double[1];
//        yay[0] = 1.1d;
//        HelloWorld hello = new HelloWorld();
//        hello.instanceMethod(yay);
//        System.out.println("Successfully succeed: " + yay[0]);
//        double d2 = 2 * d;
//        System.out.println(d2);
//        long l = 1L;
//        long l2 = 2 * l;
//        System.out.println(l2);
//        Object[] objArr = new Object[1];
//        objArr[0] = 1.1d;
//        System.out.println("Successfully succeed: " + objArr[0]);
//        String[] test = new String[1];
//        test[0] = "Yay!";
//        System.out.println("Successfully succeed: " + test[0]);
//
        Object string = new Object();
        System.out.println(string.toString());
        string = String.valueOf(1.1d);
        System.out.println("Successfully succeedded, amazing piece of code: " + string);
//        strArray[0] = "Hello";
//        System.out.println(strArray[0]); // Should print "Hello"
//        System.out.println("HelloWorld!");
//        int i = 0;
//        System.out.println(i);
//        int x = 1;
//        int[] ar = new int[3];
//        for (; i < 3; i++) {
//            dumbPrint(i, x);
//            x++;
//            ar[i] = x;
//            System.out.println(ar.length);
//        }
//        dumbPrint(i, ar[0]);
    }

//    public void instanceMethod(double[] d){
//        System.out.println(d[0]);
//    }
//    public static void dumbPrint(int i, int x) {
//        System.out.println("this is a method ");
//        System.out.println(i);
//        System.out.println(x);
//        foo();
//    }
//
//    public static void foo() {
//        System.out.println("StaticMethod call works :)");
//    }
}
