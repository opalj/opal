package interfaces;

interface Super {
    default void foo() {
        System.out.println("Super");
    }
}

interface Sub1 extends Super {
    abstract void foo();
}

interface Sub2 extends Super {}

interface SubSub extends Sub2, Sub1 {
    // foo is abstract here, must be overridden by implementers!
}

public class Interfaces implements SubSub {

    // Required, as foo is abstract otherwise (Sub1.foo is the maximally specific superinterface
    // method)
    public void foo() { System.out.println("Interfaces"); }

    public static void main(String[] args) {
        new Interfaces().foo();
    }
}

interface Sub3 extends Super {}

interface SubSub2 extends Sub2, Sub3 {
    // Super.foo is available here as a default method
}

abstract class Superclass1 implements Sub1 {
    // foo is abstract here
}

abstract class Subclass1 extends Superclass1 implements Super {
    // foo is still abstract here, as the one from Sub1 is the maximally specific interface method!
}

class Superclass2 implements Super {}

abstract class Subclass2 extends Superclass2 implements Sub1 {
    // foo is made abstract by implementing Sub1
}