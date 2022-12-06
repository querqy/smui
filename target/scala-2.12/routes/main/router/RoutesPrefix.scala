// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/cjm/_cjm/CurrentNotes/osc/yy_repos/smui/conf/routes
// @DATE:Mon Sep 12 21:16:37 EDT 2022


package router {
  object RoutesPrefix {
    private var _prefix: String = "/"
    def setPrefix(p: String): Unit = {
      _prefix = p
    }
    def prefix: String = _prefix
    val byNamePrefix: Function0[String] = { () => prefix }
  }
}
