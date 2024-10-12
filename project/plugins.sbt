addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")
// For ScalaPB 0.11.x:
libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin"           % "0.11.11",
  "com.thesamet.scalapb" %% "scalapb-validate-codegen" % "0.3.1"
)
