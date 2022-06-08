/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package mutable

/**
 * This is a scala worksheet to demonstrate how to use UShortSets.
 *
 * @author Michael Eichberg
 */
object UShortSetDemo {

    val empty = UShortSet.empty                   //> empty  : org.opalj.collection.mutable.UShortSet = UShortSet()
    val just0 = UShortSet(0)                      //> just0  : org.opalj.collection.mutable.UShortSet = UShortSet(0)
    val _0_2 = just0 + 2                          //> _0_2  : org.opalj.collection.mutable.UShortSet = UShortSet(0,2)
    _0_2 + 0                                      //> res0: org.opalj.collection.mutable.UShortSet = UShortSet(0,2)
    _0_2 + 2                                      //> res1: org.opalj.collection.mutable.UShortSet = UShortSet(0,2)
    val _0_1_2 = _0_2 + 1                         //> _0_1_2  : org.opalj.collection.mutable.UShortSet = UShortSet(0,1,2)
    val _0_1_2_65535 = _0_1_2 + 65535             //> _0_1_2_65535  : org.opalj.collection.mutable.UShortSet = UShortSet(0,1,2,65
                                                  //| 535)
    _0_1_2_65535 + 65535                          //> res2: org.opalj.collection.mutable.UShortSet = UShortSet(0,1,2,65535)
    _0_1_2_65535 + 0                              //> res3: org.opalj.collection.mutable.UShortSet = UShortSet(0,1,2,65535)
    _0_1_2_65535 + 1                              //> res4: org.opalj.collection.mutable.UShortSet = UShortSet(0,1,2,65535)
    _0_1_2_65535 + 2                              //> res5: org.opalj.collection.mutable.UShortSet = UShortSet(0,1,2,65535)

    val _10_20_30_40 = UShortSet(10) + 30 + 40 + 20
                                                  //> _10_20_30_40  : org.opalj.collection.mutable.UShortSet = UShortSet(10,20,30
                                                  //| ,40)
    val _10_30_35_40 = UShortSet(10) + 30 + 40 + 35
                                                  //> _10_30_35_40  : org.opalj.collection.mutable.UShortSet = UShortSet(10,30,35
                                                  //| ,40)
    val _5_10_30_40 = UShortSet(10) + 30 + 40 + 5 //> _5_10_30_40  : org.opalj.collection.mutable.UShortSet = UShortSet(5,10,30,4
                                                  //| 0)

    val large = _5_10_30_40 + 35 + 500 + 2 + 90 + 5242 + 0 + 1 + 0 + 5 + 30
                                                  //> large  : org.opalj.collection.mutable.UShortSet = UShortSet(0,1,2,5,10,30,3
                                                  //| 5,40,90,500,5242)
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
    large.max                                     //> res19: org.opalj.UShort = 5242

    _10_20_30_40 + 0                              //> res20: org.opalj.collection.mutable.UShortSet = UShortSet(0,10,20,30,40)
    _10_20_30_40 + 5                              //> res21: org.opalj.collection.mutable.UShortSet = UShortSet(5,10,20,30,40)
    _10_20_30_40 + 15                             //> res22: org.opalj.collection.mutable.UShortSet = UShortSet(10,15,20,30,40)
    _10_20_30_40 + 25                             //> res23: org.opalj.collection.mutable.UShortSet = UShortSet(10,20,25,30,40)
    _10_20_30_40 + 35                             //> res24: org.opalj.collection.mutable.UShortSet = UShortSet(10,20,30,35,40)
    _10_20_30_40 + 45                             //> res25: org.opalj.collection.mutable.UShortSet = UShortSet(10,20,30,40,45)

    UShortSet(12, 23)                             //> res26: org.opalj.collection.mutable.UShortSet = UShortSet(12,23)
    UShortSet(12, 12)                             //> res27: org.opalj.collection.mutable.UShortSet = UShortSet(12)
    UShortSet(23, 11)                             //> res28: org.opalj.collection.mutable.UShortSet = UShortSet(11,23)

    try {
        empty + 66666
    } catch {
        case _: IllegalArgumentException => "OK"
    }                                             //> res29: Object = OK

    try {
        empty + -1
    } catch {
        case _: IllegalArgumentException => "OK"
    }                                             //> res30: Object = OK

    UShortSet.create(0, 5, 3, 10, 19, 200, 65, 56, 56, 3, 0).size == 8
                                                  //> res31: Boolean = true

    _0_1_2_65535.map(_.toString)                  //> res32: scala.collection.mutable.Set[String] = Set(0, 1, 2, 65535)

    (_10_20_30_40 + 10) eq _10_20_30_40           //> res33: Boolean = true

    (_0_1_2.mutableCopy) ne _0_1_2                //> res34: Boolean = true
    (_10_20_30_40.mutableCopy) eq _10_20_30_40    //> res35: Boolean = true

    var x = _10_20_30_40                          //> x  : org.opalj.collection.mutable.UShortSet = UShortSet(10,20,30,40)
    x += 50
    x += 60
    x                                             //> res36: org.opalj.collection.mutable.UShortSet = UShortSet(10,20,30,40,50,60
                                                  //| )
    val y = 60 +≈: (50 +≈: (_10_20_30_40))        //> y  : org.opalj.collection.mutable.UShortSet = UShortSet(10,20,30,40,50,60)
    x == y                                        //> res37: Boolean = true
    x ne y                                        //> res38: Boolean = true

    val theSet = UShortSet.empty                  //> theSet  : org.opalj.collection.mutable.UShortSet = UShortSet()

    theSet +                                      //> res39: org.opalj.collection.mutable.UShortSet = UShortSet()
    61149 +                                       //> res40: Int = 61149
    61154 +                                       //> res41: Int = 61154
    61158                                         //> res42: Int = 61158
}