// We have centralized the configuration of all plug-ins here, to make this file easily
// useable by the dockerfile to configure the docker image used for building OPAL.

// to build fat-jars
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.7")

resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.5.1")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.1")
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.2")

addSbtPlugin("com.eed3si9n" % "sbt-dirty-money" % "0.2.0")

// to make it possible to check for outdated dependencies
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.4")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// FOR THE DEPLOYMENT TO MAVEN CENTRAL
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3")

//
// CONFIGURATION OF SCALARIFORM
// TODO Use scalariform to automatically format the build files!
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.2")
