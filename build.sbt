import com.typesafe.sbt.GitBranchPrompt

name := "search-management-ui"
version := "4.3.0"
maintainer := "Contact productful.io <hello@productful.io>"

scalaVersion := "2.13.14"

ThisBuild / evictionErrorLevel := Level.Info

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(GitBranchPrompt)
  .enablePlugins(sbtdocker.DockerPlugin)
  .settings(
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoOptions += BuildInfoOption.ToJson,
    buildInfoPackage := "models.buildInfo",
    buildInfoKeys := Seq[BuildInfoKey](name, version, "gitHash" -> git.gitHeadCommit.value.getOrElse("emptyRepository")),
    watchSources ++= (baseDirectory.value / "frontend/src" ** "*").get
  )
  .settings(dependencyCheckSettings: _*)

updateOptions := updateOptions.value.withCachedResolution(cachedResolution = true)

lazy val dependencyCheckSettings: Seq[Setting[_]] = {
  import DependencyCheckPlugin.autoImport._
  Seq(
    dependencyCheckSuppressionFile := Some(new File("suppress-checks.xml").getAbsoluteFile),
    dependencyCheckFormats := Seq("HTML", "JSON"),
    dependencyCheckAssemblyAnalyzerEnabled := Some(false)
  )
}

resolvers ++= Seq(
  Resolver.jcenterRepo,
  Resolver.bintrayRepo("renekrie", "maven"),
  "Shibboleth releases" at "https://build.shibboleth.net/nexus/content/repositories/releases/"
)

lazy val JacksonVersion = "2.15.2"
lazy val Pac4jVersion = "6.0.4"

lazy val JacksonCoreExclusion     = ExclusionRule(organization = "com.fasterxml.jackson.core")

// we can omit bcprov-jdk15on as we also have bcprov-jdk18on as dependency
lazy val BcProv15Exclusion        = ExclusionRule(organization = "org.bouncycastle", name = "bcprov-jdk15on")

// we can omit spring-jcl as Play provides jcl-over-slf4j
lazy val SpringJclBridgeExclusion = ExclusionRule(organization = "org.springframework", name = "spring-jcl")

libraryDependencies ++= {
  Seq(
    guice,
    jdbc,
    evolutions,
    "com.jayway.jsonpath" % "json-path" % "2.7.0",
    "org.querqy" % "querqy-core" % "3.7.0", // querqy dependency
    "ch.qos.logback" % "logback-classic" % "1.4.8",
    "net.logstash.logback" % "logstash-logback-encoder" % "5.3", // JSON logging:
    "org.codehaus.janino" % "janino" % "3.0.8", // For using conditions in logback.xml:
    "mysql" % "mysql-connector-java" % "8.0.18", // TODO verify use of mysql-connector over explicit mariaDB connector instead
    "org.postgresql" % "postgresql" % "42.5.1",
    "org.xerial" % "sqlite-jdbc" % "3.40.0.0",
    "org.playframework.anorm" %% "anorm" % "2.7.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % JacksonVersion,
    "org.apache.commons" % "commons-csv" % "1.10.0",
    "org.apache.shiro" % "shiro-core" % "1.12.0",
    "org.pac4j" % "pac4j-http" % Pac4jVersion excludeAll (JacksonCoreExclusion, BcProv15Exclusion, SpringJclBridgeExclusion),
    "org.pac4j" % "pac4j-saml" % Pac4jVersion excludeAll (JacksonCoreExclusion, BcProv15Exclusion, SpringJclBridgeExclusion),
    "org.pac4j" %% "play-pac4j" % "12.0.0-PLAY3.0",
    "com.google.cloud" % "google-cloud-storage" % "2.33.0",
    "org.scalatest" %% "scalatest" % "3.2.18" % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
    "org.scalatestplus" %% "mockito-5-10" % "3.2.18.0" % Test,
    "com.h2database" % "h2" % "1.4.197" % Test, // H2 DB for testing
    // Other databases as docker containers for testing with specific databases
    "com.dimafeng" %% "testcontainers-scala" % "0.40.11" % Test,
    "org.testcontainers" % "postgresql" % "1.17.6" % Test,
    "org.testcontainers" % "mysql" % "1.17.6" % Test
  )
}

dependencyOverrides ++= {
  Seq(
    "com.fasterxml.jackson.core" % "jackson-annotations" % JacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-core" % JacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % JacksonVersion
  )
}

assembly / mainClass := Some("play.core.server.ProdServerStart")
assembly / fullClasspath += Attributed.blank(PlayKeys.playPackageAssets.value)

assembly / assemblyMergeStrategy := {
  case manifest if manifest.contains("MANIFEST.MF") =>
    // We don't need manifest files since sbt-assembly will create
    // one with the given settings
    MergeStrategy.discard
  case x if x.endsWith("module-info.class") => MergeStrategy.discard
  // Protobuf schemas, we just use the first one as we don't use Protobuf at all
  case x if x.endsWith(".proto") => MergeStrategy.first
  case "play/reference-overrides.conf" => MergeStrategy.concat
  case PathList("META-INF", "spring.factories") => MergeStrategy.concat
  case PathList("META-INF", "spring", "aot.factories") => MergeStrategy.concat
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

lazy val dockerNamespace = "querqy"
lazy val dockerRepo = "smui"

docker / imageNames := {
  val semVerLevels = version.value.split('.')
  val majorVersion = semVerLevels.head
  val minorVersion = semVerLevels.drop(1).head

  // create tags for 'latest', and all SemVer levels
  Seq("latest", majorVersion, s"$majorVersion.$minorVersion", version.value).map { tag =>
    ImageName(
      namespace = Some(dockerNamespace),
      repository = dockerRepo,
      tag = Some(tag)
    )
  }
}

docker / dockerfile := NativeDockerfile(baseDirectory.value / "Dockerfile")

docker / dockerBuildArguments := Map(
  "VERSION" -> version.value,
)

docker / buildOptions := BuildOptions(
  pullBaseImage = BuildOptions.Pull.Always
)

// Fix build on Mac M1 ("Apple Silicon") chipsets (see https://discuss.lightbend.com/t/apple-silicon-m1-playframework-broken-on-apple-silicon/7924/16)
// TODO using jdk8 instead (to avoid `java.lang.IllegalStateException: Unable to load cache item`)
PlayKeys.fileWatchService := play.dev.filewatch.FileWatchService.jdk7(play.sbt.run.toLoggerProxy(sLog.value))
