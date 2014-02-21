package de.tud.cs.st
package bat
package resolved

object UIDListDemo {

    val o1 = ObjectType("o1")                     //> o1  : de.tud.cs.st.bat.resolved.ObjectType = ObjectType(o1)
    val o2 = ObjectType("o2")                     //> o2  : de.tud.cs.st.bat.resolved.ObjectType = ObjectType(o2)
    val o3 = ObjectType("o3")                     //> o3  : de.tud.cs.st.bat.resolved.ObjectType = ObjectType(o3)
    val o4 = ObjectType("o4")                     //> o4  : de.tud.cs.st.bat.resolved.ObjectType = ObjectType(o4)

    val s1 = UIDList(o1)                          //> s1  : de.tud.cs.st.bat.UIDList[de.tud.cs.st.bat.resolved.ObjectType] = UIDSL
                                                  //| ist(ObjectType(o1))
    val s2 = UIDList(o2)                          //> s2  : de.tud.cs.st.bat.UIDList[de.tud.cs.st.bat.resolved.ObjectType] = UIDSL
                                                  //| ist(ObjectType(o2))
    val s12 = s1 + o2                             //> s12  : de.tud.cs.st.bat.UIDList[de.tud.cs.st.bat.resolved.ObjectType] = UIDS
                                                  //| List(ObjectType(o1),ObjectType(o2))
    val s21 = s2 + o1                             //> s21  : de.tud.cs.st.bat.UIDList[de.tud.cs.st.bat.resolved.ObjectType] = UIDS
                                                  //| List(ObjectType(o1),ObjectType(o2))
    s21 == s12                                    //> res0: Boolean = true

    UIDList(o1, o3) == UIDList(o3, o1)            //> res1: Boolean = true

    s12.map(_.id)                                 //> res2: List[Any] = List(21, 22)

    s12.filter(_.id < 20).map(_.id)               //> res3: List[Any] = List()

    s12.filterNot(_.id < 20).map(_.id)            //> res4: List[Any] = List(21, 22)

    !s12.exists(_ == o3)                          //> res5: Boolean = true

    s12.exists(_ == o1)                           //> res6: Boolean = true
    s12.exists(_ == o2)                           //> res7: Boolean = true
    s12.forall(_.id >= 0)                         //> res8: Boolean = true
    s12.find(_.id > 10).isDefined                 //> res9: Boolean = true
    !s12.find(_.id < 10).isDefined                //> res10: Boolean = true

    val se: UIDList[ObjectType] = UIDList.empty   //> se  : de.tud.cs.st.bat.UIDList[de.tud.cs.st.bat.resolved.ObjectType] = UIDSL
                                                  //| ist()
    !se.exists(_ == o2)                           //> res11: Boolean = true
    !se.contains(o2)                              //> res12: Boolean = true
    se.filter(_.id < 20).map(_.id)                //> res13: List[Any] = List()
    se.filterNot(_.id < 20).map(_.id)             //> res14: List[Any] = List()

    s12.filter(_.id < 100) eq s12                 //> res15: Boolean = true
    s12.filterNot(_.id > 100) eq s12              //> res16: Boolean = true

    val s1234 = s12 + o3 + o4                     //> s1234  : de.tud.cs.st.bat.UIDList[de.tud.cs.st.bat.resolved.ObjectType] = UI
                                                  //| DSList(ObjectType(o1),ObjectType(o2),ObjectType(o3),ObjectType(o4))
    s1234.iterator.map(_.id).mkString(", ")       //> res17: String = 21, 22, 23, 24
    s1234.foldLeft(0)(_ + _.id)                   //> res18: Int = 90
    s2.foldLeft(o1.id)(_ + _.id)                  //> res19: Int = 43

}