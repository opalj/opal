/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package type_annotations;

import java.io.Serializable;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * This class tests some (corner) cases related to type annotations.
 *
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 *
 * @author Michael Eichberg
 */
public abstract class RITypeAnnotationUser<@RITypeAnnotation T extends @RITypeAnnotation Serializable & Cloneable>
        implements List<@RITypeAnnotation Object>, @RITypeAnnotation("superinterface") Serializable {

    public   @RITypeAnnotation("on nested class declaration") class Nested {

        public  @RITypeAnnotation("on nested class declaration") class SubNested {

            public   @RITypeAnnotation("on nested class declaration") class SubSubNested {

            }

        }

    }

    private static final Object process(Object o) {return o;} // just a helper method

    private static final @RITypeAnnotation("field declaration") long serialVersionUID = 1L;

    public List<@RITypeAnnotation("annotation of generic type parameter") T> ser;

    @SuppressWarnings("all")
    public Object onNestedTypeAnnotations() throws Exception {
        Nested nested =  new RITypeAnnotationUser.@RITypeAnnotation("on nested class instantiation") Nested();
        Nested.SubNested subNested = nested.new @RITypeAnnotation("on nested^2 class instantiation") SubNested();
        Nested.SubNested.SubSubNested subSubNested = subNested.new @RITypeAnnotation("on nested^2 class instantiation") SubSubNested();

        return subSubNested;
    }

    public Object localVariableDeclarationTypeAnnotations() throws Exception {
        @RITypeAnnotation("local variable annotation")
        List<@RITypeAnnotation("type parameter annotation") T> l = new ArrayList<>();
        return l;
    }

    public void wildcardsRelatedTypeAnnotation() throws Exception {
        List<@RITypeAnnotation ? extends Cloneable> l = new ArrayList<>();
        process(l);
    }

    @SuppressWarnings("unchecked")
    public void arrayRelatedTypeAnnotation() throws Exception {
        @RITypeAnnotation("array as such") int[] @RITypeAnnotation("2nd dimension of array")[] ls = new int[10][1];
        process(ls);

        List<@RITypeAnnotation("type parameter annotation of array of generic type") Serializable>[] lgs = new List[10];
        process(lgs);
    }

    public Supplier<Vector<?>> instanceCreationRelatedTypeAnnotation() throws Exception {
        List<?> l = new @RITypeAnnotation ArrayList<>();
        process(l);
        return (@RITypeAnnotation Vector::new);
    }

    public Supplier<Object> methodCallRelatedTypeAnnotation() throws Exception {
        return (@RITypeAnnotation("receiver class") System::lineSeparator);
    }

    public @RITypeAnnotation("return type") Function<@RITypeAnnotation("first parameter of generic type") Object,  @RITypeAnnotation("second parameter of generic type") Integer> methodSignatureRelatedTypeAnnotations() throws Exception {
        List<Object> l = new ArrayList<>();
        return (l::indexOf);
    }

    @SuppressWarnings("all")
    public <X extends @RITypeAnnotation Serializable & @RITypeAnnotation("annotation of second type of intersection type") Cloneable> void typeCheckRelatedTypeAnnotations(X x) {
        // annotated type test
        if(x instanceof @RITypeAnnotation("annotated instanceof") List) {
            List<X> l = (@RITypeAnnotation("annotated type cast") List< @RITypeAnnotation("annotated generic type variable in (unsafe) type cast")X>) x;
            process(l);
        }

        // annotated type cast
        if(x instanceof List) {
            Object l = (Serializable & @RITypeAnnotation("annotation of second type of a case to an intersection type") Cloneable) x;
            process(l);
        }
    }

    public String doIt(File file) throws Exception{
        try (
            @RITypeAnnotation("resource variable") BufferedReader br =
                new BufferedReader(new FileReader(file))
        ) {
            return br.readLine();
        }
    }


    public String doArray(File[] file) throws Exception{
        @RITypeAnnotation("the array")File[] newFiles =  file.clone();

        Object[]@RITypeAnnotation("an inner array")[] os = new @RITypeAnnotation("a new array") Object[10]@RITypeAnnotation("a new inner array")[5];
        process(os);

        return newFiles[0].toString();
    }
}
