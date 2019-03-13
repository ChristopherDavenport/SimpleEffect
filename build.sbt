lazy val core = project.in(file("core"))
    .settings(commonSettings)
    .settings(
      name := "simple-effect"
    )

lazy val test = project.in(file("test"))
  .settings(commonSettings, catsSettings)

lazy val cats = project.in(file("cats"))
  .settings(commonSettings, catsSettings)
  .dependsOn(core)
  .settings(
    name := "simple-effect-cats"
  )

val catsV = "1.6.0"
val catsEffectV = "1.2.0"

val specs2V = "4.2.0"
val disciplineV = "0.11.0"

lazy val contributors = Seq(
  "ChristopherDavenport" -> "Christopher Davenport"
)

lazy val commonSettings = Seq(
  organization := "io.chrisdavenport",

  scalaVersion := "2.12.8",
  crossScalaVersions := Seq(scalaVersion.value, "2.11.12"),

  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.9" cross CrossVersion.binary),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0-M4"),

  libraryDependencies ++= Seq(
    "org.typelevel"               %% "cats-effect-laws"           % catsEffectV   % Test,
    "org.typelevel"               %% "cats-testkit"               % catsV % Test
  )
)

lazy val catsSettings = Seq(
  libraryDependencies ++= Seq(
    "org.typelevel"               %% "cats-effect"                % catsEffectV
  )
)



lazy val releaseSettings = {
  import ReleaseTransformations._
  Seq(
    releaseCrossBuild := true,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      // For non cross-build projects, use releaseStepCommand("publishSigned")
      releaseStepCommandAndRemaining("+publishSigned"),
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges
    ),
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    credentials ++= (
      for {
        username <- Option(System.getenv().get("SONATYPE_USERNAME"))
        password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
      } yield
        Credentials(
          "Sonatype Nexus Repository Manager",
          "oss.sonatype.org",
          username,
          password
        )
    ).toSeq,
    publishArtifact in Test := false,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/ChristopherDavenport/SimpleEffect"),
        "git@github.com:ChristopherDavenport/SimpleEffect.git"
      )
    ),
    homepage := Some(url("https://github.com/ChristopherDavenport/SimpleEffect")),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    publishMavenStyle := true,
    pomIncludeRepository := { _ =>
      false
    },
    pomExtra := {
      <developers>
        {for ((username, name) <- contributors) yield
        <developer>
          <id>{username}</id>
          <name>{name}</name>
          <url>http://github.com/{username}</url>
        </developer>
        }
      </developers>
    }
  )
}
