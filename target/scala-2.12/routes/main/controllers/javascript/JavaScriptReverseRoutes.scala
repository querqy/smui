// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/cjm/_cjm/CurrentNotes/osc/yy_repos/smui/conf/routes
// @DATE:Mon Sep 12 21:16:37 EDT 2022

import play.api.routing.JavaScriptReverseRoute


import _root_.controllers.Assets.Asset

// @LINE:8
package controllers.javascript {

  // @LINE:9
  class ReverseHealthController(_prefix: => String) {

    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:9
    def health: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.HealthController.health",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "health"})
        }
      """
    )
  
  }

  // @LINE:8
  class ReverseFrontendController(_prefix: => String) {

    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:44
    def assetOrDefault: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.FrontendController.assetOrDefault",
      """
        function(file0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + (""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("file", file0)})
        }
      """
    )
  
    // @LINE:8
    def index: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.FrontendController.index",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + """"})
        }
      """
    )
  
  }

  // @LINE:13
  class ReverseApiController(_prefix: => String) {

    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:30
    def getLatestDeploymentResult: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.getLatestDeploymentResult",
      """
        function(solrIndexId0,targetSystem1) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/log/deployment-info" + _qS([(""" + implicitly[play.api.mvc.QueryStringBindable[String]].javascriptUnbind + """)("solrIndexId", solrIndexId0), (""" + implicitly[play.api.mvc.QueryStringBindable[String]].javascriptUnbind + """)("targetSystem", targetSystem1)])})
        }
      """
    )
  
    // @LINE:16
    def addNewSolrIndex: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.addNewSolrIndex",
      """
        function() {
          return _wA({method:"PUT", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/solr-index"})
        }
      """
    )
  
    // @LINE:28
    def downloadAllRulesTxtFiles: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.downloadAllRulesTxtFiles",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/allRulesTxtFiles"})
        }
      """
    )
  
    // @LINE:18
    def listAllInputTags: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.listAllInputTags",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/inputTags"})
        }
      """
    )
  
    // @LINE:38
    def getActivityReport: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.getActivityReport",
      """
        function(solrIndexId0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/report/activity-report/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("solrIndexId", solrIndexId0))})
        }
      """
    )
  
    // @LINE:40
    def getDatabaseJsonWithId: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.getDatabaseJsonWithId",
      """
        function(id0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/get-export-with-id/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("id", id0))})
        }
      """
    )
  
    // @LINE:32
    def addNewSpelling: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.addNewSpelling",
      """
        function(solrIndexId0) {
          return _wA({method:"PUT", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("solrIndexId", solrIndexId0)) + "/spelling"})
        }
      """
    )
  
    // @LINE:33
    def updateSpelling: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.updateSpelling",
      """
        function(solrIndexId0,canonicalSpellingId1) {
          return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("solrIndexId", solrIndexId0)) + "/spelling/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("canonicalSpellingId", canonicalSpellingId1))})
        }
      """
    )
  
    // @LINE:39
    def getLatestVersionInfo: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.getLatestVersionInfo",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/version/latest-info"})
        }
      """
    )
  
    // @LINE:20
    def getDetailedSearchInput: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.getDetailedSearchInput",
      """
        function(searchInputId0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/search-input/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("searchInputId", searchInputId0))})
        }
      """
    )
  
    // @LINE:35
    def getDetailedSpelling: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.getDetailedSpelling",
      """
        function(canonicalSpellingId0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/spelling/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("canonicalSpellingId", canonicalSpellingId0))})
        }
      """
    )
  
    // @LINE:27
    def deleteSuggestedSolrField: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.deleteSuggestedSolrField",
      """
        function(solrIndexId0,suggestedFieldId1) {
          return _wA({method:"DELETE", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("solrIndexId", solrIndexId0)) + "/suggested-solr-field/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("suggestedFieldId", suggestedFieldId1))})
        }
      """
    )
  
    // @LINE:24
    def updateRulesTxtForSolrIndexAndTargetPlatform: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.updateRulesTxtForSolrIndexAndTargetPlatform",
      """
        function(solrIndexId0,targetPlatform1) {
          return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("solrIndexId", solrIndexId0)) + "/rules-txt/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("targetPlatform", targetPlatform1))})
        }
      """
    )
  
    // @LINE:26
    def addNewSuggestedSolrField: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.addNewSuggestedSolrField",
      """
        function(solrIndexId0) {
          return _wA({method:"PUT", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("solrIndexId", solrIndexId0)) + "/suggested-solr-field"})
        }
      """
    )
  
    // @LINE:29
    def importFromRulesTxt: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.importFromRulesTxt",
      """
        function(solrIndexId0) {
          return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("solrIndexId", solrIndexId0)) + "/import-from-rules-txt"})
        }
      """
    )
  
    // @LINE:21
    def addNewSearchInput: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.addNewSearchInput",
      """
        function(solrIndexId0) {
          return _wA({method:"PUT", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("solrIndexId", solrIndexId0)) + "/search-input"})
        }
      """
    )
  
    // @LINE:41
    def uploadImport: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.uploadImport",
      """
        function() {
          return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/upload-import"})
        }
      """
    )
  
    // @LINE:37
    def getRulesReport: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.getRulesReport",
      """
        function(solrIndexId0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/report/rules-report/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("solrIndexId", solrIndexId0))})
        }
      """
    )
  
    // @LINE:17
    def deleteSolrIndex: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.deleteSolrIndex",
      """
        function(solrIndexId0) {
          return _wA({method:"DELETE", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/solr-index/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("solrIndexId", solrIndexId0))})
        }
      """
    )
  
    // @LINE:22
    def updateSearchInput: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.updateSearchInput",
      """
        function(searchInputId0) {
          return _wA({method:"POST", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/search-input/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("searchInputId", searchInputId0))})
        }
      """
    )
  
    // @LINE:19
    def listAllSearchInputs: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.listAllSearchInputs",
      """
        function(solrIndexId0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("solrIndexId", solrIndexId0)) + "/search-input"})
        }
      """
    )
  
    // @LINE:23
    def deleteSearchInput: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.deleteSearchInput",
      """
        function(searchInputId0) {
          return _wA({method:"DELETE", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/search-input/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("searchInputId", searchInputId0))})
        }
      """
    )
  
    // @LINE:34
    def deleteSpelling: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.deleteSpelling",
      """
        function(canonicalSpellingId0) {
          return _wA({method:"DELETE", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/spelling/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("canonicalSpellingId", canonicalSpellingId0))})
        }
      """
    )
  
    // @LINE:31
    def listAll: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.listAll",
      """
        function(solrIndexId0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("solrIndexId", solrIndexId0)) + "/rules-and-spellings"})
        }
      """
    )
  
    // @LINE:13
    def getFeatureToggles: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.getFeatureToggles",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/featureToggles"})
        }
      """
    )
  
    // @LINE:15
    def getSolrIndex: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.getSolrIndex",
      """
        function(solrIndexId0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/solr-index/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("solrIndexId", solrIndexId0))})
        }
      """
    )
  
    // @LINE:36
    def getActivityLog: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.getActivityLog",
      """
        function(inputId0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/log/rule-activity-log" + _qS([(""" + implicitly[play.api.mvc.QueryStringBindable[String]].javascriptUnbind + """)("inputId", inputId0)])})
        }
      """
    )
  
    // @LINE:14
    def listAllSolrIndeces: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.listAllSolrIndeces",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/solr-index"})
        }
      """
    )
  
    // @LINE:25
    def listAllSuggestedSolrFields: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "controllers.ApiController.listAllSuggestedSolrFields",
      """
        function(solrIndexId0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/v1/" + encodeURIComponent((""" + implicitly[play.api.mvc.PathBindable[String]].javascriptUnbind + """)("solrIndexId", solrIndexId0)) + "/suggested-solr-field"})
        }
      """
    )
  
  }


}
