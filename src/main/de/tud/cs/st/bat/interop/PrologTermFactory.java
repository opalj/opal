/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
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
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat.interop;

import java.util.List;

/**
 * This interface serves as the interopability layer between the BAT
 * and any possible Java clients. 
 *
 * @since 23.11.2009 11:13:41
 * @author Sebastian Harrte
 */
public interface PrologTermFactory<Fact, Term, Atom extends Term> {

    Atom KeyAtom(String prefix);

    Atom IntegerAtom(long value);

    Atom FloatAtom(double value);

    /**
     * String atoms are never quoted; they have to be legal prolog atoms out
     * of the box. From the runtime point of view both
     * text atoms and StringAtoms are indistinguishable. However, when
     * printed TextAtoms are quoted, while StringAtoms are not.
     */
    Atom StringAtom(String s);

    /**
     * Text atoms are always quoted. From the runtime point of view both
     * text atoms and StringAtoms are indistinguishable. However, when
     * printed TextAtoms are quoted, while StringAtoms are not.
     */
    Atom TextAtom(String text);

    /**
     * Creates a term that represents a list of terms.
     */
    Term Terms(List<Term> terms);

    Term Term(String functor, List<Term> terms);

    Fact Fact(String functor, List<Term> terms);

    Atom NoneAtom();

    Atom YesAtom();

    Atom NoAtom();

    Atom PublicAtom();

    Atom PrivateAtom();

    Atom ProtectedAtom();

    Atom DefaultAtom();

    Term VolatileTerm(boolean isVolatile);

    Term VarargsTerm(boolean isVarargs);

    Term AbstractTerm(boolean isAbstract);

    Term BridgeTerm(boolean isBridge);

    Term NativeTerm(boolean isNative);

    Term StaticTerm(boolean isStatic);

    Term FinalTerm(boolean isFinal);

    Term StrictTerm(boolean isStrict);

    Term SynchronizedTerm(boolean isSynchronized);

    Term TransientTerm(boolean isTransient);

    Term SyntheticTerm(boolean isSynthetic);

    Term DeprecatedTerm(boolean isDeprecated);
}
