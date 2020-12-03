import com.typesafe.sbt.GitBranchPrompt

name := "search-management-ui"
version := "3.12.0"

scalaVersion := "2.12.11"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(GitBranchPrompt)
  .settings(
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoOptions += BuildInfoOption.ToJson,
    buildInfoPackage := "models.buildInfo",
    buildInfoKeys := Seq[BuildInfoKey](name, version, "gitHash" -> git.gitHeadCommit.value.getOrElse("emptyRepository"))
  )
  .settings(dependencyCheckSettings: _*)

updateOptions := updateOptions.value.withCachedResolution(cachedResoluton = true)
// we use nodejs to make our typescript build as fast as possible
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

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
  Resolver.bintrayRepo("webjars", "maven"),
  Resolver.bintrayRepo("renekrie", "maven")
)

libraryDependencies ++= {
  val ngVersion="5.1.0"
  Seq(

    // Play Framework Dependencies

    guice,
    jdbc,
    evolutions,
    
    // JSON logging:
    "net.logstash.logback" % "logstash-logback-encoder" % "5.3",
    // For using conditions in logback.xml:
    "org.codehaus.janino" % "janino" % "3.0.8",

    // querqy dependency

    "org.querqy" % "querqy-core" % "3.7.0",

    // Additional Play Framework Dependencies

    "mysql" % "mysql-connector-java" % "8.0.18", // TODO verify use of mysql-connector over explicit mariaDB connector instead
    "org.postgresql" % "postgresql" % "42.2.5",
    "org.xerial" % "sqlite-jdbc" % "3.25.2",
    "org.playframework.anorm" %% "anorm" % "2.6.4",
    "com.typesafe.play" %% "play-json" % "2.6.12",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0" % Test,
    "org.mockito" % "mockito-all" % "1.10.19" % Test,

    // angular2 Dependencies

    "org.webjars.npm" % "angular__common" % ngVersion,
    "org.webjars.npm" % "angular__compiler" % ngVersion,
    "org.webjars.npm" % "angular__core" % ngVersion,
    "org.webjars.npm" % "angular__http" % ngVersion,
    "org.webjars.npm" % "angular__forms" % ngVersion,
    "org.webjars.npm" % "angular__router" % ngVersion,
    "org.webjars.npm" % "angular__animations" % ngVersion,
    "org.webjars.npm" % "angular__platform-browser-dynamic" % ngVersion,
    "org.webjars.npm" % "angular__platform-browser" % ngVersion,
    "org.webjars.npm" % "systemjs" % "0.20.14",
    "org.webjars.npm" % "rxjs" % "5.4.2",
    "org.webjars.npm" % "reflect-metadata" % "0.1.8",
    "org.webjars.npm" % "zone.js" % "0.8.4",
    "org.webjars.npm" % "core-js" % "2.4.1",
    "org.webjars.npm" % "symbol-observable" % "1.0.1",

    "org.webjars.npm" % "typescript" % "2.4.1",

    "org.webjars.npm" % "ng-bootstrap__ng-bootstrap" % "1.0.0",
    "org.webjars.npm" % "angular2-toaster" % "2.0.0", // TODO consider native Angular2/Bootstrap "growl" or "toast" library
    "org.webjars.npm" % "tokenize2" % "1.3.0", // Tag input

    "org.webjars" % "jquery" % "3.2.1",
    "org.webjars" % "bootstrap" % "4.0.0-beta.2",
    "org.webjars.bower" % "fontawesome" % "4.7.0",

    // tslint dependency

    "org.webjars.npm" % "tslint-eslint-rules" % "3.4.0",
    "org.webjars.npm" % "tslint-microsoft-contrib" % "4.0.0",
    // "org.webjars.npm" % "codelyzer" % "3.1.1", see below
    "org.webjars.npm" % "types__jasmine" % "2.5.53" % Test,

    // test
    "org.webjars.npm" % "jasmine-core" % "2.6.4" % Test,

    // H2 DB for testing
    "com.h2database" % "h2" % "1.4.197" % Test,

    // For jwt token parsing
    "com.pauldijou" %% "jwt-play" % "4.1.0",

    // Other databases as docker containers for testing with specific databases
    "com.dimafeng" %% "testcontainers-scala" % "0.32.0" % Test,
    "org.testcontainers" % "postgresql" % "1.12.1" % Test,
    "org.testcontainers" % "mysql" % "1.12.1" % Test,
    "org.xerial" % "sqlite-jdbc" % "3.28.0" % Test
  )
}

dependencyOverrides ++= {
  lazy val jacksonVersion = "2.9.10"
  Seq(
    "org.webjars.npm" % "minimatch" % "3.0.0",
    "org.webjars.npm" % "glob" % "7.1.2",
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion
  )
}

// use the webjars npm directory (target/web/node_modules ) for resolution of module imports of angular2/core etc
resolveFromWebjarsNodeModulesDir := true

// compile our tests as commonjs instead of systemjs modules
(projectTestFile in typescript) := Some("tsconfig.test.json")

// use the combined tslint and eslint rules plus ng2 lint rules
(rulesDirectories in tslint) := Some(List(
  tslintEslintRulesDir.value,
  ng2LintRulesDir.value //codelyzer uses 'cssauron' which can't resolve 'through' see https://github.com/chrisdickinson/cssauron/pull/10
))

mainClass in assembly := Some("play.core.server.ProdServerStart")
fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)

assemblyMergeStrategy in assembly := {
  case manifest if manifest.contains("MANIFEST.MF") =>
    // We don't need manifest files since sbt-assembly will create
    // one with the given settings
    MergeStrategy.discard
  case "module-info.class" => MergeStrategy.discard
  case "play/reference-overrides.conf" => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

// the naming conventions of our test files
//jasmineFilter in jasmine := GlobFilter("*Test.js") | GlobFilter("*Spec.js") | GlobFilter("*.spec.js")
//logLevel in jasmine := Level.Info
logLevel in tslint := Level.Info
logLevel in typescript := Level.Info
