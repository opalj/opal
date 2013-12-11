package de.tud.cs.st.bat.test.invokedynamic.annotations;

import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;

@Retention(RUNTIME)
@Target(METHOD)
public @interface InvokeDynamicField {
	Class<?> declaringType();
	String name();
	Class<?> fieldType();
	boolean isStatic() default false;
}
