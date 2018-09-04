// Required to compile the Java projects that are used as test fixtures.
// THE VERSION OF THIS LIBRARY MUST NOT BE UPDATED - WE RELY ON THE JAVA CLASS FILE'S INTERNALS!
libraryDependencies += "org.eclipse.jdt.core.compiler" % "ecj" % "4.6.1"  // <= DO *NOT* CHANGE!

// Required to compile the website
libraryDependencies += "org.scalatra.scalate" %% "scalate-core" % "1.9.0"
libraryDependencies += "com.vladsch.flexmark" % "flexmark-all" % "0.34.24"
libraryDependencies += "com.typesafe" % "config" % "1.3.3"
