addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.3.0")

resolvers += Resolver.url(
  "sbt-plugin-releases", 
  new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/")
)(Resolver.ivyStylePatterns)
