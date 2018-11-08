name := "short-url"

version := "1.0"

scalaVersion := "2.12.1"
val akkaHttp = "10.1.3"

resolvers += Resolver.jcenterRepo
libraryDependencies += "com.typesafe.akka" %% "akka-http" % akkaHttp
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.14"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.3"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.14"

// Tools
libraryDependencies += "commons-validator" % "commons-validator" % "1.6"

// Logger
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

// Test dependencies
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
libraryDependencies += "net.andreinc.mockneat" % "mockneat" % "0.2.2" % Test

mainClass in Compile := Some("com.github.dmgcodevil.shorturl.Application")