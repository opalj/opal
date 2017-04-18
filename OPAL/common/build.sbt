name := "Common"

// We don't want the build to be aborted by inter-project links that are reported by
// scaladoc as errors using the standard compiler setting. (This case is only true, when
// we publish the projects.)
scalacOptions in (Compile, doc) := Opts.doc.title("OPAL-Common")
scalacOptions in (Compile, console) := Seq("-deprecation")

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.5.14"

//[FICUS HAS MOVED TO COM.IHEART]libraryDependencies += "net.ceedubs" %% "ficus" % "1.1.2"
libraryDependencies += "com.iheart" %% "ficus" % "1.4.0"
