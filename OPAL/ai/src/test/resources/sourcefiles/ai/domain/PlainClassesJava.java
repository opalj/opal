/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package ai.domain;

/**
 * This class's methods contain do computations related to Class objects to test the
 * ClassValues resolution.
 * 
 * @author Arne Lottmann
 */
public class PlainClassesJava {

    public Class<?> staticClassValue() {
        return String.class;
    }

    public Class<?> noLiteralStringInClassForName(String s) throws ClassNotFoundException {
        return Class.forName(s, false, this.getClass().getClassLoader());
    }

    public Class<?> literalStringInLongClassForName() throws ClassNotFoundException {
        return Class
                .forName("java.lang.Integer", false, this.getClass().getClassLoader());
    }

    public Class<?> literalStringInClassForName() throws ClassNotFoundException {
        return Class.forName("java.lang.Integer");
    }

    public Class<?> stringVariableInClassForName() throws ClassNotFoundException {
        String className = "java.lang.Integer";
        return Class.forName(className);
    }

    public Class<?> literalStringAsParameterInClassForName()
            throws ClassNotFoundException {
        return getClass("java.lang.Integer");
    }

    private Class<?> getClass(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }

    public Class<?> getClassOrElseObject(String className) throws ClassNotFoundException {
        if (className != null) {
            return Class.forName(className);
        }

        return Object.class;
    }

    public Object getClassByKey(int i) throws Exception {
        Class<?> c = null;
        switch (i) {
        case 0:
            c = Class.forName("java.util.Set");
            break;
        case 1:
            c = Class.forName("java.util.List");
            break;
        default:
            c = Object.class;
        }
        return c.newInstance();
    }

    public Object getClassByKeyAlt(int i) throws Exception {
        String s;
        switch (i) {
        case 0:
            s = ("java.util.Set");
            break;
        case 1:
            s = ("java.util.List");
            break;
        default:
            s = "java.lang.Object";
        }
        Class<?> c = Class.forName(s);
        return c.newInstance();
    }

    public Class<?> stringVariableAsParameterInClassForName()
            throws ClassNotFoundException {
        String className = "java.lang.Integer";
        return getClass(className);
    }
}
