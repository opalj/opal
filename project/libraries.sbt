// Required to compile the Java projects that are used as test fixtures.
// THE VERSION OF THIS LIBRARY MUST NOT BE UPDATED - WE RELY ON THE JAVA CLASS FILE'S INTERNALS!
libraryDependencies += "org.eclipse.jdt" % "ecj" % "3.26.0.v20210317-0507" // <= DO *NOT* CHANGE!

// Required to compile the website
libraryDependencies += "com.typesafe.play" %% "twirl-api" % "1.5.1"
libraryDependencies += "com.typesafe.play" %% "twirl-compiler" % "1.5.1"
libraryDependencies += "org.scala-lang" % "scala-compiler" % sbt.Keys.scalaVersion.value

libraryDependencies += "com.vladsch.flexmark" % "flexmark-all"  % "0.62.2"
libraryDependencies += "com.typesafe"         % "config"        % "1.4.1"
