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
package analyses

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers

import reader.Java7Framework.ClassFiles

/**
 * Basic tests of the class hierarchy.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ClassHierarchyTest
        extends FlatSpec
        with ShouldMatchers /*with BeforeAndAfterAll */ {

    //
    //
    // Setup
    //
    //
    val ch = ClassHierarchy.createPreInitializedClassHierarchy()

    //
    //
    // Verify
    //
    //

    behavior of "The ClassHierarchy created using \"readPredefinedHierarchy\""

    it should "correctly reflect the base exception hierarchy" in {
        val Object = ObjectType("java/lang/Object")
        val Throwable = ObjectType("java/lang/Throwable")
        val Exception = ObjectType("java/lang/Exception")
        val Error = ObjectType("java/lang/Error")
        val RuntimeException = ObjectType("java/lang/RuntimeException")

        ch.isSubtypeOf(Object, Throwable) should be(Some(false))

        ch.isSubtypeOf(Throwable, Object) should be(Some(true))

        ch.isSubtypeOf(Error, Throwable) should be(Some(true))

        ch.isSubtypeOf(RuntimeException, Exception) should be(Some(true))

        ch.isSubtypeOf(Exception, Throwable) should be(Some(true))
    }
}
