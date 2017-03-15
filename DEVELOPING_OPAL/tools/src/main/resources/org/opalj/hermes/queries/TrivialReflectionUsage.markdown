#Trivial Reflection Usage

Counts the number of cases where `Class.forName` calls can be trivially resolved, because the respective String(s) are directly available. E.g.,

    String className = "com.here.MyLAF"
    Class.forName(className)

or

    String  className = "com.here.DefaultLAF"
    if(<some condition>)
            className = "com.here.OtherLAF"
    Class.forName(className)
