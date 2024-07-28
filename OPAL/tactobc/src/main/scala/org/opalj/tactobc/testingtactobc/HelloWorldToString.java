package org.opalj.tactobc.testingtactobc;

public class HelloWorldToString {

    public static void main(String[] args) {
        System.out.println("HelloWorld!");
        int i = 0;
        System.out.println(i);
        int x = 1;
        for(; i < 3; i++){
            dumbPrint(i,x);
            x++;
        }
        dumbPrint(i,x);
    }

    public static void dumbPrint(int i, int x){
        System.out.println("this is a method ".concat(String.valueOf(i)));
        System.out.println(i);
        System.out.println(x);
        foo();
    }

    public static void foo(){
        System.out.println("StaticMethod call works :)");
    }
}

