
name := """demand_forecasting"""

version := "1.0"
scalaVersion := "2.11.11"

val akkaVersion = "2.5.3"
val akkaHttpVersion = "10.0.8"
val reactiveKafkaVersion = "0.16"
val kafkaVersion = "0.10.2.1"
val scalaKafkaClientVersion = "0.10.2.2"
val playVersion = "2.5.15"
val scalatestVersion = "3.0.1"
val sparkVersion = "2.1.1"
val tensorflowVersion = "1.2.1"
val akkaStreamAlpakkaCassandraVersion = "0.9"
val jacksonVersion = "2.8.8"
val nscalaTimeVersion = "2.16.0"
val scalaLoggingVersion = "3.5.0"
val cassandraVersion = "3.3.0"

val commonSettings = Seq(
  scalaVersion := "2.11.11",
  resolvers += "Apache Snapshot Repository" at "https://repository.apache.org/snapshots",
  resolvers += Resolver.bintrayRepo("cakesolutions", "maven"),
  resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases",
    fork in run := true,

  libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % akkaVersion,
        "com.typesafe.akka" %% "akka-stream" % akkaVersion,
        "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
        "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
        "com.typesafe.akka" %% "akka-stream-kafka" % reactiveKafkaVersion,
        "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
        "org.scalatest" %% "scalatest" % scalatestVersion % "test",
        "org.apache.kafka" %% "kafka" % kafkaVersion,
        "com.typesafe.play" %% "play-json" % playVersion,
        "com.typesafe.play" % "play_2.11" % playVersion,
        "com.typesafe.play" % "play-test_2.11" % playVersion,
        "org.apache.spark" % "spark-core_2.11" % sparkVersion,
        "org.apache.spark" % "spark-streaming_2.11" % sparkVersion,
        "org.apache.spark" % "spark-mllib_2.11" % sparkVersion,
        "org.tensorflow" % "tensorflow" % tensorflowVersion,
        "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % jacksonVersion,
        "net.cakesolutions" %% "scala-kafka-client" % scalaKafkaClientVersion,
        "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
        "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
        "com.github.nscala-time" %% "nscala-time" % nscalaTimeVersion,
        "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % akkaStreamAlpakkaCassandraVersion,
        "com.datastax.cassandra"  % "cassandra-driver-core" % cassandraVersion exclude("org.slf4j", "log4j-over-slf4j")
  )
)
lazy val frontend = (project in file("frontend"))
  .settings(commonSettings)
  .enablePlugins(PlayScala)
lazy val backend = (project in file("backend"))
  .settings(commonSettings)


