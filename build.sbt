name := "zio-intro"

version := "0.1"

scalaVersion := "2.13.3"

val zioVersion = "1.0.1"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion,
  "dev.zio" %% "zio-test"          % zioVersion % "test",
  "dev.zio" %% "zio-test-sbt"      % zioVersion % "test",
)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

