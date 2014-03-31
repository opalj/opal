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
package de.tud.cs.st.collection.mutable

/**
 * This is a scala worksheet to demonstrate how to use UShortSets.
 *
 * @author Michael Eichberg
 */
object UShortSetDemo {

    val empty = UShortSet.empty                   //> empty  : de.tud.cs.st.collection.mutable.UShortSet = UShortSet()
    val just0 = UShortSet(0)                      //> just0  : de.tud.cs.st.collection.mutable.UShortSet = UShortSet(0)
    val _0_2 = just0 + 2                          //> _0_2  : de.tud.cs.st.collection.mutable.UShortSet = UShortSet(0,2)
    _0_2 + 0                                      //> res0: de.tud.cs.st.collection.mutable.UShortSet = UShortSet(0,2)
    _0_2 + 2                                      //> res1: de.tud.cs.st.collection.mutable.UShortSet = UShortSet(0,2)
    val _0_1_2 = _0_2 + 1                         //> _0_1_2  : de.tud.cs.st.collection.mutable.UShortSet = UShortSet(0,1,2)
    val _0_1_2_65535 = _0_1_2 + 65535             //> _0_1_2_65535  : de.tud.cs.st.collection.mutable.UShortSet = UShortSet(0,1,2
                                                  //| ,65535)
    _0_1_2_65535 + 65535                          //> res2: de.tud.cs.st.collection.mutable.UShortSet = UShortSet(0,1,2,65535)
    _0_1_2_65535 + 0                              //> res3: de.tud.cs.st.collection.mutable.UShortSet = UShortSet(0,1,2,65535)
    _0_1_2_65535 + 1                              //> res4: de.tud.cs.st.collection.mutable.UShortSet = UShortSet(0,1,2,65535)
    _0_1_2_65535 + 2                              //> res5: de.tud.cs.st.collection.mutable.UShortSet = UShortSet(0,1,2,65535)

    val _10_20_30_40 = UShortSet(10) + 30 + 40 + 20
                                                  //> _10_20_30_40  : de.tud.cs.st.collection.mutable.UShortSet = UShortSet(10,20
                                                  //| ,30,40)
    val _10_30_35_40 = UShortSet(10) + 30 + 40 + 35
                                                  //> _10_30_35_40  : de.tud.cs.st.collection.mutable.UShortSet = UShortSet(10,30
                                                  //| ,35,40)
    val _5_10_30_40 = UShortSet(10) + 30 + 40 + 5 //> _5_10_30_40  : de.tud.cs.st.collection.mutable.UShortSet = UShortSet(5,10,3
                                                  //| 0,40)

    val large = _5_10_30_40 + 35 + 500 + 2 + 90 + 5242 + 0 + 1 + 0 + 5 + 30
                                                  //> large  : de.tud.cs.st.collection.mutable.UShortSet = UShortSet(0,1,2,5,10,3
                                                  //| 0,35,40,90,500,5242)
    large.contains(0)                             //> res6: Boolean = true
    large.contains(1)                             //> res7: Boolean = true
    large.contains(2)                             //> res8: Boolean = true
    large.contains(5)                             //> res9: Boolean = true
    large.contains(10)                            //> res10: Boolean = true
    large.contains(30)                            //> res11: Boolean = true
    large.contains(35)                            //> res12: Boolean = true
    large.contains(40)                            //> res13: Boolean = true
    large.contains(90)                            //> res14: Boolean = true
    large.contains(500)                           //> res15: Boolean = true
    large.contains(5242)                          //> res16: Boolean = true
    !large.contains(4)                            //> res17: Boolean = true
    !large.contains(6666)                         //> res18: Boolean = true
    large.max                                     //> res19: Int = 5242

    _10_20_30_40 + 0                              //> res20: de.tud.cs.st.collection.mutable.UShortSet = UShortSet(0,10,20,30,40)
                                                  //| 
    _10_20_30_40 + 5                              //> res21: de.tud.cs.st.collection.mutable.UShortSet = UShortSet(5,10,20,30,40)
                                                  //| 
    _10_20_30_40 + 15                             //> res22: de.tud.cs.st.collection.mutable.UShortSet = UShortSet(10,15,20,30,40
                                                  //| )
    _10_20_30_40 + 25                             //> res23: de.tud.cs.st.collection.mutable.UShortSet = UShortSet(10,20,25,30,40
                                                  //| )
    _10_20_30_40 + 35                             //> res24: de.tud.cs.st.collection.mutable.UShortSet = UShortSet(10,20,30,35,40
                                                  //| )
    _10_20_30_40 + 45                             //> res25: de.tud.cs.st.collection.mutable.UShortSet = UShortSet(10,20,30,40,45
                                                  //| )

    UShortSet(12, 23)                             //> res26: de.tud.cs.st.collection.mutable.UShortSet = UShortSet(12,23)
    UShortSet(12, 12)                             //> res27: de.tud.cs.st.collection.mutable.UShortSet = UShortSet(12)
    UShortSet(23, 11)                             //> res28: de.tud.cs.st.collection.mutable.UShortSet = UShortSet(11,23)

    try {
        empty + 66666
    } catch {
        case _: IllegalArgumentException ⇒ "OK"
    }                                             //> res29: Object = OK

    try {
        empty + -1
    } catch {
        case _: IllegalArgumentException ⇒ "OK"
    }                                             //> res30: Object = OK

    UShortSet.create(0, 5, 3, 10, 19, 200, 65, 56, 56, 3, 0).size == 8
                                                  //> res31: Boolean = true

    _0_1_2_65535.map(_.toString)                  //> res32: scala.collection.mutable.Set[String] = Set(0, 1, 2, 65535)

}