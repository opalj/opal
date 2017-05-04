// Required to compile the Java projects that are used as test fixtures.
// THE VERSION OF THIS LIBRARY MUST NOT BE UPDATED - THE JAVA CLASS FILE'S INTERNALS ARE USED!
libraryDependencies += "org.eclipse.jdt.core.compiler" % "ecj" % "4.6.1"  // <= DO NOT CHANGE!

// Required to compile the website
libraryDependencies += "org.fusesource.scalate" %% "scalate-core" % "1.6.1"
libraryDependencies += "com.vladsch.flexmark" % "flexmark-all" % "0.19.1"
libraryDependencies += "com.typesafe" % "config" % "1.3.1"
