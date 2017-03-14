#Trivial Reflection Usage

Counts the number of cases where `Class.forName` calls can be trivially resolved, because the respective String is directly available. E.g.,

    String className = "com.here.MyLAF"
    Class.forName(className)
