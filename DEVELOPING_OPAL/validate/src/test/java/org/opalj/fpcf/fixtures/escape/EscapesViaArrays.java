/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
