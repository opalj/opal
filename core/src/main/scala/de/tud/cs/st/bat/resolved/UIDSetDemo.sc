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
package de.tud.cs.st
package bat
package resolved

import de.tud.cs.st.collection._

object UIDSetDemo {

    val o1 = ObjectType("o1")                     //> o1  : de.tud.cs.st.bat.resolved.ObjectType = ObjectType(o1)
    val o2 = ObjectType("o2")                     //> o2  : de.tud.cs.st.bat.resolved.ObjectType = ObjectType(o2)
    val o3 = ObjectType("o3")                     //> o3  : de.tud.cs.st.bat.resolved.ObjectType = ObjectType(o3)
    val o4 = ObjectType("o4")                     //> o4  : de.tud.cs.st.bat.resolved.ObjectType = ObjectType(o4)
    val o5 = ObjectType("o5")                     //> o5  : de.tud.cs.st.bat.resolved.ObjectType = ObjectType(o5)
    val o6 = ObjectType("o6")                     //> o6  : de.tud.cs.st.bat.resolved.ObjectType = ObjectType(o6)
    val o7 = ObjectType("o7")                     //> o7  : de.tud.cs.st.bat.resolved.ObjectType = ObjectType(o7)

    val s1 = UIDSet(o1)                           //> s1  : de.tud.cs.st.collection.UIDSet[de.tud.cs.st.bat.resolved.ObjectType] 
                                                  //| = UIDSet(22:ObjectType(o1))
    val s2 = UIDSet(o2)                           //> s2  : de.tud.cs.st.collection.UIDSet[de.tud.cs.st.bat.resolved.ObjectType] 
                                                  //| = UIDSet(23:ObjectType(o2))
    val s12 = s1 + o2                             //> s12  : de.tud.cs.st.collection.UIDSet[de.tud.cs.st.bat.resolved.ObjectType]
                                                  //|  = UIDSet(22:ObjectType(o1),23:ObjectType(o2))
    val s21 = s2 + o1                             //> s21  : de.tud.cs.st.collection.UIDSet[de.tud.cs.st.bat.resolved.ObjectType]
                                                  //|  = UIDSet(22:ObjectType(o1),23:ObjectType(o2))
    s21 == s12                                    //> res0: Boolean = true

    UIDSet(o1, o3) == UIDSet(o3, o1)              //> res1: Boolean = true

    s12.map(_.id)                                 //> res2: Set[Int] = Set(22, 23)
 
    s12.filter(_.id < 20)                         //> res3: de.tud.cs.st.collection.UIDSet[de.tud.cs.st.bat.resolved.ObjectType] 
                                                  //| = UIDSet()
    s12.filter(_.id < 23)                         //> res4: de.tud.cs.st.collection.UIDSet[de.tud.cs.st.bat.resolved.ObjectType] 
                                                  //| = UIDSet(22:ObjectType(o1))

    s12.filterNot(_.id < 20)                      //> res5: de.tud.cs.st.collection.UIDSet[de.tud.cs.st.bat.resolved.ObjectType] 
                                                  //| = UIDSet(22:ObjectType(o1),23:ObjectType(o2))
    s12.filterNot(_.id < 23)                      //> res6: de.tud.cs.st.collection.UIDSet[de.tud.cs.st.bat.resolved.ObjectType] 
                                                  //| = UIDSet(23:ObjectType(o2))

    !s12.exists(_ == o3)                          //> res7: Boolean = true

    s12.exists(_ == o1)                           //> res8: Boolean = true
    s12.exists(_ == o2)                           //> res9: Boolean = true
    s12.forall(_.id >= 0)                         //> res10: Boolean = true
    s12.find(_.id > 10).isDefined                 //> res11: Boolean = true
    !s12.find(_.id < 10).isDefined                //> res12: Boolean = true

    val se = UIDSet.empty[ObjectType]             //> se  : de.tud.cs.st.collection.UIDSet[de.tud.cs.st.bat.resolved.ObjectType] 
                                                  //| = UIDSet()
    !se.exists(_ == o2)                           //> res13: Boolean = true
    !se.contains(o2)                              //> res14: Boolean = true
    se.filter(_.id < 20).map(_.id)                //> res15: Set[Int] = Set()
    se.filterNot(_.id < 20).map(_.id)             //> res16: Set[Int] = Set()

    s12.filter(_.id < 100) eq s12                 //> res17: Boolean = false
    s12.filterNot(_.id > 100) eq s12              //> res18: Boolean = false

    val s1234 = s12 + o3 + o4                     //> s1234  : de.tud.cs.st.collection.UIDSet[de.tud.cs.st.bat.resolved.ObjectTyp
                                                  //| e] = UIDSet(22:ObjectType(o1),23:ObjectType(o2),24:ObjectType(o3),25:Object
                                                  //| Type(o4))
    s1234.map(_.id).mkString(", ")                //> res19: String = 22, 23, 24, 25
    s1234.foldLeft(0)(_ + _.id)                   //> res20: Int = 94
    s2.foldLeft(o1.id)(_ + _.id)                  //> res21: Int = 45

    s1234 + o7 + o5 + o6                          //> res22: de.tud.cs.st.collection.UIDSet[de.tud.cs.st.bat.resolved.ObjectType]
                                                  //|  = UIDSet(22:ObjectType(o1),23:ObjectType(o2),24:ObjectType(o3),25:ObjectTy
                                                  //| pe(o4),26:ObjectType(o5),27:ObjectType(o6),28:ObjectType(o7))

}