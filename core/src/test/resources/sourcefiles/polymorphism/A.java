package polymorphism;

public class A {

    private A a = this;

    public void m() {
        a.foo(); // here, a refers to an object of type B if bar was called before m()
        a.foo(); // here, a "always" refers to an object of type B and not this!
    }

    protected void foo() {
        a = new B();
    }

    public void bar() {
        a = new B();
    }
}

class B extends A {

    protected void foo() {
        bar();
    }

    public void bar() {
        // do nothing
    }
}