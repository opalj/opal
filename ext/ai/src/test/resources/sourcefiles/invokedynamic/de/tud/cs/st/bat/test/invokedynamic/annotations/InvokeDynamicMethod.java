package de.tud.cs.st.bat.test.invokedynamic.annotations;

import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;

@Retention(RUNTIME)
@Target(METHOD)
public @interface InvokeDynamicMethod {
	Class<?> receiverType();
	String name();
	Class<?> returnType();
	Class<?>[] parameterTypes() default {};
}
