package de.tud.cs.st.bat.resolved.dependency

object DependencyTypeSheet {

    val dt_1 = DependencyType.EXTENDS             //> dt_1  : de.tud.cs.st.bat.resolved.dependency.DependencyType.Value = type dec
                                                  //| laration EXTENDS class type

    val dt_1m = DependencyType.bitMask(dt_1)      //> dt_1m  : Long = 1

    DependencyType.toSet(1l)                      //> res0: scala.collection.Set[de.tud.cs.st.bat.resolved.dependency.DependencyTy
                                                  //| pe] = Set(type declaration EXTENDS class type)

    val dt_35 = DependencyType.USES_TYPE_IN_TYPE_PARAMETERS
                                                  //> dt_35  : de.tud.cs.st.bat.resolved.dependency.DependencyType.Value = uses ty
                                                  //| pe in type parameters
    val dt_35m = DependencyType.bitMask(dt_35)    //> dt_35m  : Long = 34359738368

    dt_35.id                                      //> res1: Int = 35

    val dt_1_35m = dt_1m | dt_35m                 //> dt_1_35m  : Long = 34359738369

    DependencyType.toSet(dt_1_35m)                //> res2: scala.collection.Set[de.tud.cs.st.bat.resolved.dependency.DependencyTy
                                                  //| pe] = Set(type declaration EXTENDS class type, uses type in type parameters)
                                                  //| 

}