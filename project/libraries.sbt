// Required to compile the Java projects that are used as test fixtures.
// THE VERSION OF THIS LIBRARY MUST NOT BE UPDATED - WE RELY ON THE JAVA CLASS FILE'S INTERNALS!
libraryDependencies += "org.eclipse.jdt" % "ecj" % "3.24.0" // <= DO *NOT* CHANGE!

// Required to compile the website
libraryDependencies += "org.scalatra.scalate" %% "scalate-core" % "1.9.6"
libraryDependencies += "com.vladsch.flexmark" % "flexmark-all"  % "0.62.2"
libraryDependencies += "com.typesafe"         % "config"        % "1.4.1"
