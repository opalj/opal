package org.opalj.fpcf.fixtures.field_mutability;

import org.opalj.fpcf.properties.field_mutability.DeclaredFinal;
import org.opalj.fpcf.properties.field_mutability.LazyInitialized;
import org.opalj.fpcf.properties.field_mutability.NonFinal;

class Simple {
    @LazyInitialized("Simple lazy initialization")
    private int x;

    public int init(){
        if(x == 0){
            x = 5;
        }
        return x;
    }
}

class Local {
    @LazyInitialized("Lazy initialization with local")
    private int x;

    public int init(){
        int y = this.x;
        if(y == 0){
            x = y = 5;
        }
        return y;
    }
}

class LocalWrong {
    @NonFinal("Incorrect lazy initialization with local")
    private int x;

    public int init(){
        int y = this.x;
        if(y == 0){
            x = 5;
        }
        return y;
    }
}

class LocalReversed {
    @LazyInitialized("Lazy initialization with local (reversed)")
    private int x;

    public int init(){
        int y = this.x;
        if(y == 0){
            y = x = 5;
        }
        return y;
    }
}

class LocalReload {
    @LazyInitialized("Lazy initialization with local (reloading the field's value after the write)")
    private int x;

    public int init(){
        int y = this.x;
        if(y == 0){
            x = 5;
            y = x;
        }
        return y;
    }
}

class SimpleReversed {
    @LazyInitialized("Simple lazy initialization (reversed)")
    private int x;

    public int init(){
        if(x != 0) return x;
        x = 5;
        return x;
    }
}

class DeterministicCall {
    @LazyInitialized("Lazy initialization with call to deterministic method")
    private int x;

    public int init(){
        if(x == 0){
            x = this.sum(5, 8);
        }
        return x;
    }

    private final int sum(int a, int b){
        return a+b;
    }
}

class VisibleInitialization {

    @NonFinal("Incorrect because lazy initialization is visible")
    private int x;

    public int init() {
        int y = this.x;
        int z;
        if (y == 0) {
            y = x = 5;
            z = 3;
        } else {
            z = 2;
        }
        System.out.println(z);
        return y;
    }
}

class ExceptionInInitialization {
        @NonFinal("Incorrect because lazy initialization is can not happen due to exception")
        private int x;

        private int getZero(){ return 0; }

        public int init(){
            int y = this.x;
            if(y == 0){
                int z = 10/getZero();
                y = x = 5;
            }
            return y;
        }
    }

class PossibleExceptionInInitialization {
    @NonFinal("Incorrect because lazy initialization is may not happen due to exception")
    private int x;

    public int init(int i){
        int y = this.x;
        if(y == 0){
            int z = 10/i;
            y = x = 5;
        }
        return y;
    }
}


class CaughtExceptionInInitialization {
    @NonFinal("Incorrect because lazy initialization is may not happen due to exception")
    private int x;

    public int init(int i){
        int y = this.x;
        try {
            if (y == 0) {
                int z = 10 / i;
                y = x = 5;
            }
            return y;
        }
        catch(Exception e){
            return 0;
        }
    }
}