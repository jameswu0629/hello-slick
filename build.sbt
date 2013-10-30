name := """hello-slick"""

version := "1.0"

scalaVersion := "2.10.2"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Maven repository" at "http://repo.springsource.org/plugins-release/"

libraryDependencies ++= List(
  "com.typesafe.slick" %% "slick" % "1.0.1",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "com.h2database" % "h2" % "1.3.166",
  "mysql" % "mysql-connector-java" % "5.1.26",
  "play" % "play_2.10" % "2.1.5",
  "voldemort" % "voldemort" % "0.96",
  "log4j" % "log4j" % "1.2.15" exclude("javax.jms", "jms") exclude("javax.jdmk", "jmxtools") exclude ("com.sun.jmx", "jmxri") exclude ("com.sun.jdmk", "jmxtools"),
  "jdom" % "jdom" % "1.1",
  "com.cloudphysics" % "jerkson_2.10" % "0.6.3",
  "ch.qos.logback" % "logback-classic" % "1.0.13",
  "ch.qos.logback" % "logback-core" % "1.0.13"
)
