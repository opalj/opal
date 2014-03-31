/* License (BSD Style License):
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package FieldIsntImmutableInImmutableClass;

import net.jcip.annotations.Immutable;

/**
 * Some class annotated with JCIP Immutable annotation. Everything is implemented
 * correctly. This should not be reported.
 * 
 * @author Roberts Kolosovs
 * @author Peter Spieler
 */
@Immutable
public class JCIPCorrectlyImplementedImmutableClass {

    public final int foo = 0;
    private NotImmutableWithPublicFields bar;
    private String bum;
    private int[] baz = { 1, 3, 3, 7 };
    private String[] werWohntHier = { "Das ", "ist ", "das ", "Haus ", "vom ", "Nikolaus" };

    // Constructor calls the private setters.
    public JCIPCorrectlyImplementedImmutableClass(NotImmutableWithPublicFields arg0,
            int[] arg1, String arg2) {
        setBum(arg2);
        setBar(arg0);
        setBaz(arg1);
        setBazWithClone(arg1);
    }

    // Setter for bar, only used by the constructor. No defensive copy needed, as strings
    // are immutable.
    private void setBum(String arg0) {
        bum = arg0;
    }

    // Getter for Bum. No defensive copy needed, as strings are immutable.
    public String getBum() {
        return bum;
    }

    // Private setter for Bar with deep defensive copy by cloning.
    private void setBar(NotImmutableWithPublicFields arg0) {
        bar = new NotImmutableWithPublicFields(arg0.x, arg0.foo.clone());
    }

    // Public getter for bar, with deep defensive copy by cloning.
    public NotImmutableWithPublicFields getBar() {
        int[] arr = new int[bar.foo.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = bar.foo[i];
        }
        NotImmutableWithPublicFields temp = new NotImmutableWithPublicFields(bar.x, arr);
        return temp;
    }

    // Setter for baz, only used by the constructor. Implemented with defensive copy.
    private void setBaz(int[] arg0) {
        int[] blub = new int[arg0.length];
        for (int i = 0; i < arg0.length; i++) {
            blub[i] = arg0[i];
        }
        baz = blub;
    }

    // Setter for baz, with defensive copy by cloning.
    private void setBazWithClone(int[] arg0) {
        baz = arg0.clone();
    }

    // Public getter for WerWohntHier, with defensive copy by cloning.
    public String[] getWerWohntHier() {
        String[] out = werWohntHier.clone();
        return out;
    }

    // Public getter for baz, implemented with defensive copy.
    public int[] getBaz() {
        int[] v = new int[baz.length];
        for (int i = 0; i < baz.length; i++) {
            v[i] = baz[i];
        }
        return v;
    }

    // No member of the class is returned. No defensive copy is needed.
    public NotImmutableWithPublicFields getParity() {
        int[] retArray = { 1, 2 };
        if (bar.x % 2 == 0) {
            return new NotImmutableWithPublicFields(1, retArray);
        } else {
            return new NotImmutableWithPublicFields(0, retArray);
        }
    }
}
