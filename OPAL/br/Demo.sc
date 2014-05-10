
object foo {
    val generalBATErrorMessage: String =
        """While reading a class file an unexpected error occured.
          |Either the class file is corrupt or internal BAT error was found.
          |The underlying problem is:
          |""".stripMargin('|')                   //> generalBATErrorMessage  : String = "While reading a class file an unexpected
                                                  //|  error occured.
                                                  //| Either the class file is corrupt or internal BAT error was found.
                                                  //| The underlying problem is:
                                                  //| "
                                                  
                                                  
    val f = new java.io.File(System.getProperty("user.home")+"/Sites/robots.txt")
                                                  //> f  : java.io.File = /Users/Michael/Sites/robots.txt
    f.getPath                                     //> res0: String = /Users/Michael/Sites/robots.txt
    f.toString                                    //> res1: String = /Users/Michael/Sites/robots.txt
                                                  
}
        