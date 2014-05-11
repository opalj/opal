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
package org.opalj
package br

import org.opalj.collection.immutable._

object UIDSetDemo {

    val o1 = ObjectType("o1")
    val o2 = ObjectType("o2")
    val o3 = ObjectType("o3")
    val o4 = ObjectType("o4")
    val o5 = ObjectType("o5")
    val o6 = ObjectType("o6")
    val o7 = ObjectType("o7")

    val s1 = UIDSet(o1)
    val s2 = UIDSet(o2)
    val s12 = s1 + o2
    val s21 = s2 + o1
    s21 == s12

    UIDSet(o1, o3) == UIDSet(o3, o1)

    s12.map(_.id)
 
    s12.filter(_.id < 20)
    s12.filter(_.id < 23)

    s12.filterNot(_.id < 20)
    s12.filterNot(_.id < 23)

    !s12.exists(_ == o3)

    s12.exists(_ == o1)
    s12.exists(_ == o2)
    s12.forall(_.id >= 0)
    s12.find(_.id > 10).isDefined
    !s12.find(_.id < 10).isDefined

    val se = UIDSet.empty[ObjectType]
    !se.exists(_ == o2)
    !se.contains(o2)
    se.filter(_.id < 20).map(_.id)
    se.filterNot(_.id < 20).map(_.id)

    s12.filter(_.id < 100) eq s12
    s12.filterNot(_.id > 100) eq s12

    val s1234 = s12 + o3 + o4
    s1234.map(_.id).mkString(", ")
    s1234.foldLeft(0)(_ + _.id)
    s2.foldLeft(o1.id)(_ + _.id)

    s1234 + o7 + o5 + o6

}