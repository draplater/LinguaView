import sbt.Package.ManifestAttributes

name := "LinguaView"

version := "1.3.5"

scalaVersion := "2.11.5"

publishMavenStyle := true

mainClass in assembly := Some("org.eclipse.jdt.internal.jarinjarloader.JarRsrcLoader")


assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case other => (assemblyMergeStrategy in assembly).value(other)
}

assemblyExcludedJars in assembly := {
  val cp = (fullClasspath in assembly).value
  val excludesJar = Set(
    "swt.jar"
  )
  cp filter { jar => excludesJar.contains(jar.data.getName)}
}

packageOptions in assembly += ManifestAttributes(
  ("Rsrc-Main-Class", "LinguaView.LoadSWT"))

packageOptions in assembly += ManifestAttributes(
  ("Rsrc-Class-Path", "./"))

unmanagedBase <<= baseDirectory { base => base / "lib" }