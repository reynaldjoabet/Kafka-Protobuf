ThisBuild / scalaVersion := "3.3.3"
ThisBuild / name         := "Kafka-Protobuf"

lazy val root = (project in file(".")).settings(
  libraryDependencies ++= Seq(
    "com.github.fd4s" %% "fs2-kafka"       % "3.5.1",
    "org.typelevel"   %% "log4cats-slf4j"  % "2.7.0",
    "ch.qos.logback"   % "logback-classic" % "1.5.8",
    "org.typelevel"   %% "cats-effect"     % "3.5.4",
    "co.fs2"          %% "fs2-core"        % "3.11.0"
  )
)

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

ThisBuild / scalacOptions ++= Seq("-no-indent")

Compile / PB.scalapbCodeGeneratorSettings += "flat_package"
