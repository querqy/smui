// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/cjm/_cjm/CurrentNotes/osc/yy_repos/smui/conf/routes
// @DATE:Mon Sep 12 21:16:37 EDT 2022

import play.api.mvc.Call


import _root_.controllers.Assets.Asset

// @LINE:8
package controllers {

  // @LINE:9
  class ReverseHealthController(_prefix: => String) {
    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:9
    def health(): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "health")
    }
  
  }

  // @LINE:8
  class ReverseFrontendController(_prefix: => String) {
    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:44
    def assetOrDefault(file:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + implicitly[play.api.mvc.PathBindable[String]].unbind("file", file))
    }
  
    // @LINE:8
    def index(): Call = {
      
      Call("GET", _prefix)
    }
  
  }

  // @LINE:13
  class ReverseApiController(_prefix: => String) {
    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:30
    def getLatestDeploymentResult(solrIndexId:String, targetSystem:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/v1/log/deployment-info" + play.core.routing.queryString(List(Some(implicitly[play.api.mvc.QueryStringBindable[String]].unbind("solrIndexId", solrIndexId)), Some(implicitly[play.api.mvc.QueryStringBindable[String]].unbind("targetSystem", targetSystem)))))
    }
  
    // @LINE:16
    def addNewSolrIndex(): Call = {
      
      Call("PUT", _prefix + { _defaultPrefix } + "api/v1/solr-index")
    }
  
    // @LINE:28
    def downloadAllRulesTxtFiles(): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/v1/allRulesTxtFiles")
    }
  
    // @LINE:18
    def listAllInputTags(): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/v1/inputTags")
    }
  
    // @LINE:38
    def getActivityReport(solrIndexId:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/v1/report/activity-report/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("solrIndexId", solrIndexId)))
    }
  
    // @LINE:40
    def getDatabaseJsonWithId(id:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/v1/get-export-with-id/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("id", id)))
    }
  
    // @LINE:32
    def addNewSpelling(solrIndexId:String): Call = {
      
      Call("PUT", _prefix + { _defaultPrefix } + "api/v1/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("solrIndexId", solrIndexId)) + "/spelling")
    }
  
    // @LINE:33
    def updateSpelling(solrIndexId:String, canonicalSpellingId:String): Call = {
      
      Call("POST", _prefix + { _defaultPrefix } + "api/v1/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("solrIndexId", solrIndexId)) + "/spelling/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("canonicalSpellingId", canonicalSpellingId)))
    }
  
    // @LINE:39
    def getLatestVersionInfo(): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/v1/version/latest-info")
    }
  
    // @LINE:20
    def getDetailedSearchInput(searchInputId:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/v1/search-input/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("searchInputId", searchInputId)))
    }
  
    // @LINE:35
    def getDetailedSpelling(canonicalSpellingId:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/v1/spelling/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("canonicalSpellingId", canonicalSpellingId)))
    }
  
    // @LINE:27
    def deleteSuggestedSolrField(solrIndexId:String, suggestedFieldId:String): Call = {
      
      Call("DELETE", _prefix + { _defaultPrefix } + "api/v1/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("solrIndexId", solrIndexId)) + "/suggested-solr-field/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("suggestedFieldId", suggestedFieldId)))
    }
  
    // @LINE:24
    def updateRulesTxtForSolrIndexAndTargetPlatform(solrIndexId:String, targetPlatform:String): Call = {
      
      Call("POST", _prefix + { _defaultPrefix } + "api/v1/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("solrIndexId", solrIndexId)) + "/rules-txt/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("targetPlatform", targetPlatform)))
    }
  
    // @LINE:26
    def addNewSuggestedSolrField(solrIndexId:String): Call = {
      
      Call("PUT", _prefix + { _defaultPrefix } + "api/v1/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("solrIndexId", solrIndexId)) + "/suggested-solr-field")
    }
  
    // @LINE:29
    def importFromRulesTxt(solrIndexId:String): Call = {
      
      Call("POST", _prefix + { _defaultPrefix } + "api/v1/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("solrIndexId", solrIndexId)) + "/import-from-rules-txt")
    }
  
    // @LINE:21
    def addNewSearchInput(solrIndexId:String): Call = {
      
      Call("PUT", _prefix + { _defaultPrefix } + "api/v1/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("solrIndexId", solrIndexId)) + "/search-input")
    }
  
    // @LINE:41
    def uploadImport(): Call = {
      
      Call("POST", _prefix + { _defaultPrefix } + "api/v1/upload-import")
    }
  
    // @LINE:37
    def getRulesReport(solrIndexId:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/v1/report/rules-report/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("solrIndexId", solrIndexId)))
    }
  
    // @LINE:17
    def deleteSolrIndex(solrIndexId:String): Call = {
      
      Call("DELETE", _prefix + { _defaultPrefix } + "api/v1/solr-index/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("solrIndexId", solrIndexId)))
    }
  
    // @LINE:22
    def updateSearchInput(searchInputId:String): Call = {
      
      Call("POST", _prefix + { _defaultPrefix } + "api/v1/search-input/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("searchInputId", searchInputId)))
    }
  
    // @LINE:19
    def listAllSearchInputs(solrIndexId:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/v1/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("solrIndexId", solrIndexId)) + "/search-input")
    }
  
    // @LINE:23
    def deleteSearchInput(searchInputId:String): Call = {
      
      Call("DELETE", _prefix + { _defaultPrefix } + "api/v1/search-input/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("searchInputId", searchInputId)))
    }
  
    // @LINE:34
    def deleteSpelling(canonicalSpellingId:String): Call = {
      
      Call("DELETE", _prefix + { _defaultPrefix } + "api/v1/spelling/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("canonicalSpellingId", canonicalSpellingId)))
    }
  
    // @LINE:31
    def listAll(solrIndexId:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/v1/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("solrIndexId", solrIndexId)) + "/rules-and-spellings")
    }
  
    // @LINE:13
    def getFeatureToggles(): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/v1/featureToggles")
    }
  
    // @LINE:15
    def getSolrIndex(solrIndexId:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/v1/solr-index/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("solrIndexId", solrIndexId)))
    }
  
    // @LINE:36
    def getActivityLog(inputId:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/v1/log/rule-activity-log" + play.core.routing.queryString(List(Some(implicitly[play.api.mvc.QueryStringBindable[String]].unbind("inputId", inputId)))))
    }
  
    // @LINE:14
    def listAllSolrIndeces(): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/v1/solr-index")
    }
  
    // @LINE:25
    def listAllSuggestedSolrFields(solrIndexId:String): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/v1/" + play.core.routing.dynamicString(implicitly[play.api.mvc.PathBindable[String]].unbind("solrIndexId", solrIndexId)) + "/suggested-solr-field")
    }
  
  }


}
