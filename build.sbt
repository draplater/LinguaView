

name := "LinguaView"

version := "1.3.5"

scalaVersion := "2.11.5"

publishMavenStyle := true

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case other => (assemblyMergeStrategy in assembly).value(other)
}

unmanagedBase <<= baseDirectory { base => base / "lib" }