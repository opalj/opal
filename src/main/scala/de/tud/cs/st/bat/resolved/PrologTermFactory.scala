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
package de.tud.cs.st.bat
package resolved

import de.tud.cs.st.prolog.{ GroundTerm, GroundTerms, Atom, Fact }

/**
 * A collection of helper methods to create atoms, terms and facts.
 *
 * @author Michael Eichberg
 */
abstract class PrologTermFactory[Fact, Term, Atom <: Term]
        extends de.tud.cs.st.prolog.PrologTermFactory[Fact, Term, Atom] {

    def KeyAtom(key: String): Atom

    def NoneAtom: Atom = StringAtom("none")

    def YesAtom: Atom = StringAtom("yes")

    def NoAtom: Atom = StringAtom("no")

    def VisibilityAtom(access_flags: Int, ctx: AccessFlagsContext): Atom = {
        if (ACC_PUBLIC ∈ access_flags)
            StringAtom("public")
        else if ((ctx ne AccessFlagsContexts.CLASS) && (ACC_PRIVATE ∈ access_flags))
            StringAtom("private")
        else if ((ctx ne AccessFlagsContexts.CLASS) && (ACC_PROTECTED ∈ access_flags))
            StringAtom("protected")
        else
            StringAtom("default")
    }

    def VolatileTerm(access_flags: Int) =
        Term("volatile", if (ACC_VOLATILE ∈ access_flags) YesAtom else NoAtom)

    def VarargsTerm(access_flags: Int) =
        Term("varargs", if (ACC_VARARGS ∈ access_flags) YesAtom else NoAtom)

    def AbstractTerm(access_flags: Int) =
        Term("abstract", if (ACC_ABSTRACT ∈ access_flags) YesAtom else NoAtom)

    def BridgeTerm(access_flags: Int) =
        Term("bridge", if (ACC_BRIDGE ∈ access_flags) YesAtom else NoAtom)

    def NativeTerm(access_flags: Int) =
        Term("native", if (ACC_NATIVE ∈ access_flags) YesAtom else NoAtom)

    def StaticTerm(access_flags: Int) =
        Term("static", if (ACC_STATIC ∈ access_flags) YesAtom else NoAtom)

    def FinalTerm(access_flags: Int) =
        Term("final", if (ACC_FINAL ∈ access_flags) YesAtom else NoAtom)

    def StrictTerm(access_flags: Int) =
        Term("strict_fp", if (ACC_STRICT ∈ access_flags) YesAtom else NoAtom)

    def SynchronizedTerm(access_flags: Int) =
        Term("synchronized", if (ACC_SYNCHRONIZED ∈ access_flags) YesAtom else NoAtom)

    def TransientTerm(access_flags: Int) =
        Term("transient", if (ACC_TRANSIENT ∈ access_flags) YesAtom else NoAtom)

    def SyntheticTerm(access_flags: Int, attributes: Attributes) = {
        Term(
            "synthetic",
            if ((ACC_SYNTHETIC ∈ access_flags)
                || (attributes contains Synthetic))
                YesAtom
            else
                NoAtom
        )
    }

    def DeprecatedTerm(attributes: Attributes) =
        Term("deprecated", if (attributes contains Deprecated) YesAtom else NoAtom)

}

