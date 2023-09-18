package org.opalj.xl.javaanalyses.benchmark.jni;

public class Native {
    public static void main(String args[]){
        int n = new C().nop();
        System.out.println(n);
    }

}

class C {
    public native int nop();
}
