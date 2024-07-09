// We have centralized the configuration of all plug-ins here, to make this file easily
// useable by the dockerfile to configure the docker image used for building OPAL.

resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(
  Resolver.ivyStylePatterns
)
addSbtPlugin("org.scoverage"   %% "sbt-scoverage"         % "2.0.9")
addSbtPlugin("org.scalameta"   % "sbt-scalafmt"           % "2.5.2")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.10.0")

addSbtPlugin("com.eed3si9n"     % "sbt-unidoc"   % "0.4.3")
addSbtPlugin("com.eed3si9n"     % "sbt-assembly" % "2.1.4")

// Dependency management:
addSbtPlugin("net.vonbuchholtz" % "sbt-dependency-check" % "3.1.1")
addSbtPlugin("com.eed3si9n"     % "sbt-dirty-money"      % "0.2.0")
addSbtPlugin("com.timushev.sbt" % "sbt-updates"          % "0.5.2")

// For the deployment to maven central:
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.7")
addSbtPlugin("com.github.sbt" % "sbt-pgp"      % "2.1.2")
