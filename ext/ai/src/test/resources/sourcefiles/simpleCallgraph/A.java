package simpleCallgraph;

import de.tud.cs.st.bat.test.invokedynamic.annotations.InvokedConstructor;
import de.tud.cs.st.bat.test.invokedynamic.annotations.InvokedMethod;
import de.tud.cs.st.bat.test.invokedynamic.annotations.InvokedMethods;

public class A implements Base {

    Base b = new B();

    @Override
    @InvokedMethod(receiverType = B.class, name = "string", lineNumber = 14)
    public String string() {
        return b.string();
    }

    @Override
    @InvokedMethod(receiverType = B.class, name = "method", lineNumber = 21)
    @InvokedConstructor(receiverType = B.class, lineNumber = 21)
    public void method() {
        new B().method();
    }

    @Override
    @InvokedMethods({
            @InvokedMethod(receiverType = A.class, name = "method", lineNumber = 29),
            @InvokedMethod(receiverType = B.class, name = "method", lineNumber = 29) })
    public void methodParameter(Base b) {
        b.method();
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = A.class, name = "method", lineNumber = 36),
            @InvokedMethod(receiverType = B.class, name = "method", lineNumber = 37) })
    public void secondMethod() {
        new A().method();
        new B().method();
    }

}
