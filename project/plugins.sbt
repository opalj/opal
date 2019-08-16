// We have centralized the configuration of all plug-ins here, to make this file easily
// useable by the dockerfile to configure the docker image used for building OPAL.

resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(
  Resolver.ivyStylePatterns
)
addSbtPlugin("org.scoverage"   %% "sbt-scoverage"         % "1.6.0")
addSbtPlugin("org.scalastyle"  %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("org.scalariform" % "sbt-scalariform"        % "1.8.2")
addSbtPlugin("org.scalameta"   % "sbt-scalafmt"           % "2.0.0")

addSbtPlugin("com.eed3si9n"     % "sbt-unidoc"   % "0.4.2")
addSbtPlugin("com.eed3si9n"     % "sbt-assembly" % "0.14.9")

// Dependency management:
addSbtPlugin("net.vonbuchholtz" % "sbt-dependency-check" % "1.3.0")
addSbtPlugin("com.eed3si9n"     % "sbt-dirty-money"      % "0.2.0")
addSbtPlugin("com.timushev.sbt" % "sbt-updates"          % "0.4.2")

// For the deployment to maven central:
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3")
addSbtPlugin("com.jsuereth"   % "sbt-pgp"      % "1.1.2")
