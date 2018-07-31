/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape;

import org.opalj.fpcf.properties.escape.*;

import java.util.*;

public class EscapesViaArrays {

    public static Circle global;

    private static final Map<String, List<Object>> staticMap =
            new @EscapeViaStaticField("assigned to a static field") HashMap<>(10);

    static {
        Object[] attributes = new @NoEscape("the array does not escape") Object[1];

        List<Object> attributesList = new
                @EscapeViaHeapObject(value = "the list is put into the static map", analyses = {})
                @AtMostEscapeInCallee(value = "is passed to map")
                        LinkedList<>();
        attributes[0] = new
                @EscapeViaHeapObject(
                        value = "the object is assigned to the list and therefore to the global map",
                        analyses = {})
                @AtMostNoEscape("we do not track the array any further")
                        Object();
        attributesList.add(attributes[0]);
        staticMap.put("Test", attributesList);
    }

    public Circle[] interestingCircles = new @EscapeViaParameter("local array") Circle[] {
            new @EscapeViaParameter(value = "local object", analyses = {}) Circle(1),
            new @EscapeViaParameter(value = "local object", analyses = {}) Circle(2)
    };

    public int[] arrayAndObjectNoEscape() {
        Circle[] circles = new @NoEscape("local array") Circle[] {
                new @NoEscape(value = "local object", analyses = {}) Circle(1),
                new @NoEscape(value = "local object", analyses = {}) Circle(2)
        };

        int[] result = new int[circles.length];
        for (int i = 0; i < circles.length; i++) {
            result[i] = circles[i].area;
        }
        return result;
    }

    public int[] objectsInCallee() {
        Circle[] circles = new @NoEscape("local array") Circle[] {
                new @EscapeInCallee(value = "local object", analyses = {}) Circle(1),
                new @EscapeInCallee(value = "local object", analyses = {}) Circle(2)
        };

        int[] result = new int[circles.length];
        for (int i = 0; i < circles.length; i++) {
            result[i] = circles[i].getArea();
        }
        return result;
    }

    public void objectsEscapeGlobal() {
        Circle[] circles = new @NoEscape("local array") Circle[] {
                new @EscapeViaStaticField(value = "local object", analyses = {}) Circle(1),
                new @EscapeViaStaticField(value = "local object", analyses = {}) Circle(2)
        };

        int i = new Random().nextInt(circles.length);
        global = circles[i];
    }

    public void objectsSimpleArraysEscapeGlobal() {
        Circle[] circles = new @NoEscape("local array") Circle[] {
                new @EscapeViaStaticField(value = "local object", analyses = {}) Circle(1),
                new @EscapeViaStaticField(value = "local object", analyses = {}) Circle(2)
        };

        global = circles[1];
    }

}
