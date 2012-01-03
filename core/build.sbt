

name := "eve-central-ng"

version := "3.0"

scalaVersion := "2.9.1"

resolvers += "Twitter" at "http://maven.twttr.com/"

resolvers += "Scala tools snapshots" at "http://scala-tools.org/repo-snapshots/"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Akka Repository" at "http://akka.io/repository/"

libraryDependencies += "se.scalablesolutions.akka" % "akka-actor" % "1.3-RC1"

libraryDependencies += "se.scalablesolutions.akka" % "akka-stm" % "1.3-RC1"

libraryDependencies += "com.twitter" % "querulous" % "2.6.5"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.6.1" % "test"

libraryDependencies += "postgresql" % "postgresql" % "9.1-901.jdbc4"

libraryDependencies += "cc.spray" % "spray-server" % "0.9.0-SNAPSHOT" changing()

libraryDependencies += "cc.spray" % "spray-client" % "0.9.0-SNAPSHOT" changing()

//libraryDependencies += "net.liftweb" % "lift-json" % "2.0"

libraryDependencies += "cc.spray.can" % "spray-can" % "0.9.2-SNAPSHOT" changing()

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.6.1"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "0.9.29"

libraryDependencies += "org.fusesource.scalate" % "scalate-core" % "1.5.3"

libraryDependencies += "cc.spray.json" %% "spray-json" % "1.0.1" % "compile" withSources()

seq(com.zentrope.ScalatePlugin.scalateTemplateSettings : _*)




