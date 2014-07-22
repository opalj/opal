package dependencies;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;

@Repeatable(TypeTestAnnotations.class)
@Target({ ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD,
        ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.PACKAGE,
        ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_PARAMETER,
        ElementType.TYPE_USE })
public @interface TypeTestAnnotation {

}
