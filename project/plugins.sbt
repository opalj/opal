// We have centralized the configuration of all plug-ins here, to make this file easily
// useable by the dockerfile to configure the docker image used for building OPAL.

// to build eclipse project configurations
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.1.0")

// to clear the ivy folders
addSbtPlugin("com.eed3si9n" % "sbt-dirty-money" % "0.1.0")

// to build fat-jars
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.4")

resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.5.0")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.0")

// to make it possible to check for outdated dependencies
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.0")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

//
// FOR THE DEPLOYMENT TO MAVEN CENTRAL
//
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")
//[0.13.x]
addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")
//[>0.13.5]
//addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
