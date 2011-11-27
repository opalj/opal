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
package javabridge

/**
 * This class adapts the PrologTermFactory java interface to the scala
 * class hierarchy for prolog term factories.
 *
 * @author Sebastian Harrte
 */
protected final class PrologTermFactoryAdapter[Fact, Term, Atom <: Term](

    val adaptee: PrologTermFactory[Fact, Term, Atom]) extends resolved.PrologTermFactory[Fact, Term, Atom] {

    def KeyAtom(s: String) = adaptee.KeyAtom(s)

    def IntegerAtom(l: Long) = adaptee.IntegerAtom(l)

    def Fact(functor: String, terms: Term*) = {
        val arglist = new java.util.ArrayList[Term](terms.size)
        var i = 0
        while (i < terms.size) {
            arglist.add(terms(i))
            i += 1
        }
        adaptee.Fact(functor, arglist)
    }

    def StringAtom(s: String) = adaptee.StringAtom(s)

    def TextAtom(s: String) = adaptee.TextAtom(s)

    def Term(functor: String, terms: Term*) = {
        val arglist = new java.util.ArrayList[Term](terms.size)
        var i = 0
        while (i < terms.size) {
            arglist.add(terms(i))
            i += 1
        }
        // TODO check if "asArray/ToArray/" is sufficient.
        adaptee.Term(functor, arglist)
    }

    def Terms[T](s: Seq[T], f: Function1[T, Term]) = {
        val terms = new java.util.ArrayList[Term](s.size)
        var i = 0
        while (i < s.size) {
            terms.add(f(s(i)))
            i += 1
        }
        adaptee.Terms(terms)
    }

    def FloatAtom(d: Double) = adaptee.FloatAtom(d)

    override def NoneAtom: Atom = adaptee.NoneAtom

    override def YesAtom: Atom = adaptee.YesAtom

    override def NoAtom: Atom = adaptee.NoAtom

    override def VisibilityAtom(access_flags: Int, ctx: AccessFlagsContext): Atom = {
        if (ACC_PUBLIC element_of access_flags)
            adaptee.PublicAtom
        else if ((ctx ne AccessFlagsContexts.CLASS) && (ACC_PRIVATE element_of access_flags))
            adaptee.PrivateAtom
        else if ((ctx ne AccessFlagsContexts.CLASS) && (ACC_PROTECTED element_of access_flags))
            adaptee.ProtectedAtom
        else
            adaptee.DefaultAtom
    }

    override def VolatileTerm(access_flags: Int) =
        adaptee.VolatileTerm(ACC_VOLATILE element_of access_flags)

    override def VarargsTerm(access_flags: Int) =
        adaptee.VarargsTerm(ACC_VARARGS element_of access_flags)

    override def AbstractTerm(access_flags: Int) =
        adaptee.AbstractTerm(ACC_ABSTRACT element_of access_flags)

    override def BridgeTerm(access_flags: Int) =
        adaptee.BridgeTerm(ACC_BRIDGE element_of access_flags)

    override def NativeTerm(access_flags: Int) =
        adaptee.NativeTerm(ACC_NATIVE element_of access_flags)

    override def StaticTerm(access_flags: Int) =
        adaptee.StaticTerm(ACC_STATIC element_of access_flags)

    override def FinalTerm(access_flags: Int) =
        adaptee.FinalTerm(ACC_FINAL element_of access_flags)

    override def StrictTerm(access_flags: Int) =
        adaptee.StrictTerm(ACC_STRICT element_of access_flags)

    override def SynchronizedTerm(access_flags: Int) =
        adaptee.SynchronizedTerm(ACC_SYNCHRONIZED element_of access_flags)

    override def TransientTerm(access_flags: Int) =
        adaptee.TransientTerm(ACC_TRANSIENT element_of access_flags)

    override def SyntheticTerm(access_flags: Int, attributes: Attributes) =
        adaptee.SyntheticTerm((ACC_SYNTHETIC element_of access_flags)
            || (attributes contains Synthetic_attribute))

    override def DeprecatedTerm(attributes: Attributes) =
        adaptee.DeprecatedTerm(attributes contains DeprecatedAttribute)

}
