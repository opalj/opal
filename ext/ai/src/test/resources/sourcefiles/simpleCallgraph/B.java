package simpleCallgraph;

import de.tud.cs.st.bat.test.invokedynamic.annotations.InvokedConstructor;
import de.tud.cs.st.bat.test.invokedynamic.annotations.InvokedMethod;
import de.tud.cs.st.bat.test.invokedynamic.annotations.InvokedMethods;

public class B implements Base {

    String b = "B";

    @Override
    public String string() {
        return b;
    }

    @Override
    @InvokedMethod(receiverType = A.class, name = "method", lineNumber = 20)
    @InvokedConstructor(receiverType = A.class, lineNumber = 20)
    public void method() {
        new A().method();
    }

    @Override
    @InvokedMethods({
            @InvokedMethod(receiverType = A.class, name = "method", lineNumber = 28),
            @InvokedMethod(receiverType = B.class, name = "method", lineNumber = 28) })
    public void methodParameter(Base b) {
        b.method();
    }

}
