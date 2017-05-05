name := """demand_forecasting"""

version := "1.0"

scalaVersion := "2.11.11"

resolvers += "Apache Snapshot Repository" at "https://repository.apache.org/snapshots"

libraryDependencies ++= {

  val akkaVersion = "2.5.0"
  val reactiveKafkaVersion = "0.15"
  val kafkaVersion = "0.10.2.0"
  val playVersion = "2.6.0-M7"
  val scalatestVersion = "3.0.1"
  val sparkVersion = "2.1.0"
  val tensorflowVersion = "1.1.0"

  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-stream-kafka" % reactiveKafkaVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion % "test",
    "org.apache.kafka" %% "kafka" % kafkaVersion,
    "com.typesafe.play" %% "play-json" % playVersion,
    "org.apache.spark" % "spark-core_2.11" % sparkVersion,
    "org.apache.spark" % "spark-streaming_2.11" % sparkVersion,
    "org.apache.spark" % "spark-mllib_2.11" % sparkVersion,
    "org.tensorflow" % "tensorflow" % tensorflowVersion,
    "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.8.8"
  )
}

fork in run := true
