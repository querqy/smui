package models

case class DeploymentScriptResult(exitCode: Int, output: String) {

  val success: Boolean = exitCode == 0

}