/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
package de.tud.cs.st
package bat

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.ParallelTestExecution

/**
 * Tests the matching of access flags.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class AccessFlagsMatcherTest
        extends FlatSpec
        with Matchers /*with BeforeAndAfterAll */
        with ParallelTestExecution {

    import AccessFlagsMatcher._

    behavior of "an AccessFlagsMatcher"

    it should "be able to correctly match a class file's access flags (PUBLIC)" in {
        val accessFlags = ACC_PUBLIC.mask

        accessFlags match {
            case ACC_PUBLIC() ⇒ /*success*/
            case _            ⇒ fail("did not match ACC_PUBLIC")
        }

        accessFlags match {
            case ACC_INTERFACE() ⇒ fail("did match ACC_INTERFACE")
            case _               ⇒ /*success*/
        }
    }

    it should "be able to correctly match a class file's access flags (PUBLIC ABSTRACT)" in {
        val afPublicAbstract = ACC_PUBLIC.mask | ACC_ABSTRACT.mask

        afPublicAbstract match {
            case ACC_PUBLIC() ⇒ /*success*/
            case _            ⇒ fail("did not match ACC_PUBLIC")
        }

        afPublicAbstract match {
            case ACC_ABSTRACT() ⇒ /*success*/
            case _              ⇒ fail("did not match ACC_ABSTRACT")
        }

        afPublicAbstract match {
            case ACC_INTERFACE() ⇒ fail("did match ACC_INTERFACE")
            case _               ⇒ /*success*/
        }

        afPublicAbstract match {
            case PUBLIC_ABSTRACT() ⇒ /*success*/
            case _                 ⇒ fail("did not match ACC_PUBLIC and ACC_ABSTRACT")
        }

        afPublicAbstract match {
            case PUBLIC_INTERFACE() ⇒ fail("did match ACC_PUBLIC & ACC_INTERFACE")
            case _                  ⇒ /*success*/
        }

        afPublicAbstract match {
            case NOT_STATIC() ⇒ /*success*/
            case _            ⇒ fail("did match ACC_STATIC")
        }

        val NOT___PUBLIC_ABSTRACT = !PUBLIC_ABSTRACT
        afPublicAbstract match {
            case NOT___PUBLIC_ABSTRACT() ⇒ fail("did match NOT (ACC_PUBLIC and ACC_ABSTRACT)")
            case _                       ⇒ /*success*/
        }

        val NOT___PUBLIC_FINAL = !PUBLIC_FINAL
        afPublicAbstract match {
            case NOT___PUBLIC_FINAL() ⇒ /*success*/
            case _                    ⇒ fail("did not match NOT (ACC_PUBLIC and ACC_FINAL)")
        }

        val PUBLIC___NOT_FINAL = ACC_PUBLIC && !ACC_FINAL
        afPublicAbstract match {
            case PUBLIC___NOT_FINAL() ⇒ /*success*/
            case _                    ⇒ fail("did not match ACC_PUBLIC and NOT (ACC_FINAL)")
        }

        val NOT_PRIVATE___NOT_FINAL = (!ACC_PRIVATE) && (!ACC_FINAL)
        afPublicAbstract match {
            case NOT_PRIVATE___NOT_FINAL() ⇒ /*success*/
            case _ ⇒
                fail(AccessFlags.toString(afPublicAbstract, AccessFlagsContexts.METHOD)+
                    " did not match "+NOT_PRIVATE___NOT_FINAL)
        }

        val NOT_NOT_PUBLIC = !(!ACC_PUBLIC)
        afPublicAbstract match {
            case NOT_NOT_PUBLIC() ⇒ /*success*/
            case _ ⇒
                fail(AccessFlags.toString(afPublicAbstract, AccessFlagsContexts.METHOD)+
                    " did not match "+NOT_NOT_PUBLIC)
        }
    }
}
