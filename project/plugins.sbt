// We have centralized the configuration of all plug-ins here, to make this file easily
// useable by the dockerfile to configure the docker image used for building OPAL.

resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(
    Resolver.ivyStylePatterns
)
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "2.3.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.5")
addSbtPlugin("com.github.sbt" % "sbt-header" % "5.11.0")

addSbtPlugin("com.github.sbt" % "sbt-unidoc" % "0.6.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.1")

// Dependency management:
addSbtPlugin("net.vonbuchholtz" % "sbt-dependency-check" % "5.1.0")
addSbtPlugin("com.eed3si9n" % "sbt-dirty-money" % "0.2.0")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.3")

// For the deployment to maven central:
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.12.2")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.1")
