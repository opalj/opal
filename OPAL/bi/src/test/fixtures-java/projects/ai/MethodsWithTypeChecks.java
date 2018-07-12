/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai;

import java.lang.Cloneable;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

/**
 * Methods that perform various kinds of type checking and casting.
 *
 * @author Michael Eichberg
 */
public class MethodsWithTypeChecks {

    // Returns either NULL or a value of type ArrayList
    public static java.util.List<Integer> get() {
        if (System.currentTimeMillis() > 0)
            return null;
        else
            return new java.util.ArrayList<Integer>();
    }

    // Regular downcast!
    @SuppressWarnings("unchecked")
    public static java.util.Collection<Object> castToCollection(Object o) {
        return (java.util.Collection<Object>) o;
    }

    // Regular downcast!
    @SuppressWarnings("unchecked")
    public static java.util.Set<Object> castToSet(Object o) {
        return (java.util.Set<Object>) o;
    }

    public static Object castToObject(Object o){return o;}

    // Either a regular downcast or a useless one.
    public static Object onceUselessOnceUsefulCast(IOException o) {
        IOException fioe = null;
        if(o == null)
            fioe = new FileNotFoundException();
        else
            fioe = o;


        return (FileNotFoundException)fioe;
    }

    public static FileNotFoundException requiredCast(Cloneable o) {
        if(!(o instanceof FileNotFoundException)) return null;

        return (FileNotFoundException)o;
    }

    // Cast guarded by exception handler.
    public static FileNotFoundException catchFailingCast(Object o) {
        try {
            return (FileNotFoundException)o;
        } catch (ClassCastException cce) {
            System.out.println(cce);
            return null;
        }
    }

    // Guarded cast which is always caught.
    public static FileNotFoundException explicitTypeCheckAndCast(Object o) {
        try {

            if(! (o instanceof FileNotFoundException) )
                throw new ClassCastException("expected FileNotFoundException");

            return (FileNotFoundException)o;

        } catch (ClassCastException cce) {
            System.out.println(cce);
            return null;
        }
    }

    @SuppressWarnings("cast")
    public static void main(String[] args) {

        java.util.List<Integer> l = get(); // <= effectively always "null"

        System.out.println(l instanceof java.util.List);
        System.out.println(null instanceof Object);
        System.out.println(l instanceof Object);
        System.out.println(l instanceof java.util.Set<?>);
        System.out.println(l instanceof File);

        java.util.Collection<Object> colL = castToCollection(l);
        java.util.Set<Object> setL = castToSet(colL);
        System.out.println(castToObject(setL) instanceof File);

        Object o = l;
        IOException ioe = (IOException) o;
        System.out.println(ioe);
        System.out.println(ioe instanceof IOException);
        java.util.List<?> list = (java.util.List<?>) o;
        System.out.println(list instanceof java.util.List<?>);

        System.out.println("End of type frenzy.");
    }
}
