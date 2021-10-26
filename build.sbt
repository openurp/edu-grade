import org.openurp.parent.Settings._
import org.openurp.parent.Dependencies._
import org.beangle.tools.sbt.Sas

ThisBuild / organization := "org.openurp.edu.grade"
ThisBuild / version := "0.0.14-SNAPSHOT"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/openurp/edu-grade"),
    "scm:git@github.com:openurp/std-grade.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id    = "chaostone",
    name  = "Tihua Duan",
    email = "duantihua@gmail.com",
    url   = url("http://github.com/duantihua")
  )
)

ThisBuild / description := "OpenURP Starter"
ThisBuild / homepage := Some(url("http://openurp.github.io/edu-grade/index.html"))

val apiVer = "0.23.2"
val baseVer = "0.1.20"
val openurp_std_api = "org.openurp.std" % "openurp-std-api" % apiVer
val openurp_edu_api = "org.openurp.edu" % "openurp-edu-api" % apiVer

lazy val root = (project in file("."))
  .settings()
  .aggregate(core)

lazy val core = (project in file("core"))
  .settings(
    name := "openurp-edu-grade-core",
    common,
    libraryDependencies ++= Seq(openurp_edu_api,openurp_std_api),
    libraryDependencies ++= Seq(beangle_data_transfer,beangle_cdi_spring,beangle_security_core,gson)
  )


publish / skip := true
