name := """demand_forecasting"""

version := "1.0"

scalaVersion := "2.11.11"

resolvers += "Apache Snapshot Repository" at "https://repository.apache.org/snapshots"
resolvers += Resolver.bintrayRepo("cakesolutions", "maven")

libraryDependencies ++= {

  val akkaVersion = "2.5.1"
  val akkaHttpVersion = "10.0.6"
  val reactiveKafkaVersion = "0.16"
  val kafkaVersion = "0.10.2.1"
  val playVersion = "2.5.15"
  val scalatestVersion = "3.0.1"
  val sparkVersion = "2.1.1"
  val tensorflowVersion = "1.2.0-rc0"

  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-stream-kafka" % reactiveKafkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion % "test",
    "org.apache.kafka" %% "kafka" % kafkaVersion,
    "com.typesafe.play" %% "play-json" % playVersion,
    "com.typesafe.play" % "play_2.11" % "2.5.15",
    "com.typesafe.play" % "play-test_2.11" % "2.5.15",
    "org.apache.spark" % "spark-core_2.11" % sparkVersion,
    "org.apache.spark" % "spark-streaming_2.11" % sparkVersion,
    "org.apache.spark" % "spark-mllib_2.11" % sparkVersion,
    "org.tensorflow" % "tensorflow" % tensorflowVersion,
    "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.8.8",
    "net.cakesolutions" %% "scala-kafka-client" % kafkaVersion,
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.8",
    "com.drewnoakes" % "metadata-extractor" % "2.10.1",
    "com.github.nscala-time" %% "nscala-time" % "2.16.0"
  )
}

fork in run := true
