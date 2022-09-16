// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/cjm/_cjm/CurrentNotes/osc/yy_repos/smui/conf/routes
// @DATE:Mon Sep 12 21:16:37 EDT 2022

package router

import play.core.routing._
import play.core.routing.HandlerInvokerFactory._

import play.api.mvc._

import _root_.controllers.Assets.Asset

class Routes(
  override val errorHandler: play.api.http.HttpErrorHandler, 
  // @LINE:8
  FrontendController_2: controllers.FrontendController,
  // @LINE:9
  HealthController_0: controllers.HealthController,
  // @LINE:13
  ApiController_1: controllers.ApiController,
  val prefix: String
) extends GeneratedRouter {

   @javax.inject.Inject()
   def this(errorHandler: play.api.http.HttpErrorHandler,
    // @LINE:8
    FrontendController_2: controllers.FrontendController,
    // @LINE:9
    HealthController_0: controllers.HealthController,
    // @LINE:13
    ApiController_1: controllers.ApiController
  ) = this(errorHandler, FrontendController_2, HealthController_0, ApiController_1, "/")

  def withPrefix(addPrefix: String): Routes = {
    val prefix = play.api.routing.Router.concatPrefix(addPrefix, this.prefix)
    router.RoutesPrefix.setPrefix(prefix)
    new Routes(errorHandler, FrontendController_2, HealthController_0, ApiController_1, prefix)
  }

  private[this] val defaultPrefix: String = {
    if (this.prefix.endsWith("/")) "" else "/"
  }

  def documentation = List(
    ("""GET""", this.prefix, """controllers.FrontendController.index()"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """health""", """controllers.HealthController.health"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/featureToggles""", """controllers.ApiController.getFeatureToggles"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/solr-index""", """controllers.ApiController.listAllSolrIndeces"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/solr-index/""" + "$" + """solrIndexId<[^/]+>""", """controllers.ApiController.getSolrIndex(solrIndexId:String)"""),
    ("""PUT""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/solr-index""", """controllers.ApiController.addNewSolrIndex"""),
    ("""DELETE""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/solr-index/""" + "$" + """solrIndexId<[^/]+>""", """controllers.ApiController.deleteSolrIndex(solrIndexId:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/inputTags""", """controllers.ApiController.listAllInputTags"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/""" + "$" + """solrIndexId<[^/]+>/search-input""", """controllers.ApiController.listAllSearchInputs(solrIndexId:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/search-input/""" + "$" + """searchInputId<[^/]+>""", """controllers.ApiController.getDetailedSearchInput(searchInputId:String)"""),
    ("""PUT""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/""" + "$" + """solrIndexId<[^/]+>/search-input""", """controllers.ApiController.addNewSearchInput(solrIndexId:String)"""),
    ("""POST""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/search-input/""" + "$" + """searchInputId<[^/]+>""", """controllers.ApiController.updateSearchInput(searchInputId:String)"""),
    ("""DELETE""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/search-input/""" + "$" + """searchInputId<[^/]+>""", """controllers.ApiController.deleteSearchInput(searchInputId:String)"""),
    ("""POST""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/""" + "$" + """solrIndexId<[^/]+>/rules-txt/""" + "$" + """targetPlatform<[^/]+>""", """controllers.ApiController.updateRulesTxtForSolrIndexAndTargetPlatform(solrIndexId:String, targetPlatform:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/""" + "$" + """solrIndexId<[^/]+>/suggested-solr-field""", """controllers.ApiController.listAllSuggestedSolrFields(solrIndexId:String)"""),
    ("""PUT""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/""" + "$" + """solrIndexId<[^/]+>/suggested-solr-field""", """controllers.ApiController.addNewSuggestedSolrField(solrIndexId:String)"""),
    ("""DELETE""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/""" + "$" + """solrIndexId<[^/]+>/suggested-solr-field/""" + "$" + """suggestedFieldId<[^/]+>""", """controllers.ApiController.deleteSuggestedSolrField(solrIndexId:String, suggestedFieldId:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/allRulesTxtFiles""", """controllers.ApiController.downloadAllRulesTxtFiles"""),
    ("""POST""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/""" + "$" + """solrIndexId<[^/]+>/import-from-rules-txt""", """controllers.ApiController.importFromRulesTxt(solrIndexId:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/log/deployment-info""", """controllers.ApiController.getLatestDeploymentResult(solrIndexId:String, targetSystem:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/""" + "$" + """solrIndexId<[^/]+>/rules-and-spellings""", """controllers.ApiController.listAll(solrIndexId:String)"""),
    ("""PUT""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/""" + "$" + """solrIndexId<[^/]+>/spelling""", """controllers.ApiController.addNewSpelling(solrIndexId:String)"""),
    ("""POST""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/""" + "$" + """solrIndexId<[^/]+>/spelling/""" + "$" + """canonicalSpellingId<[^/]+>""", """controllers.ApiController.updateSpelling(solrIndexId:String, canonicalSpellingId:String)"""),
    ("""DELETE""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/spelling/""" + "$" + """canonicalSpellingId<[^/]+>""", """controllers.ApiController.deleteSpelling(canonicalSpellingId:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/spelling/""" + "$" + """canonicalSpellingId<[^/]+>""", """controllers.ApiController.getDetailedSpelling(canonicalSpellingId:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/log/rule-activity-log""", """controllers.ApiController.getActivityLog(inputId:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/report/rules-report/""" + "$" + """solrIndexId<[^/]+>""", """controllers.ApiController.getRulesReport(solrIndexId:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/report/activity-report/""" + "$" + """solrIndexId<[^/]+>""", """controllers.ApiController.getActivityReport(solrIndexId:String)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/version/latest-info""", """controllers.ApiController.getLatestVersionInfo()"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/get-export-with-id/""" + "$" + """id<[^/]+>""", """controllers.ApiController.getDatabaseJsonWithId(id:String)"""),
    ("""POST""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/v1/upload-import""", """controllers.ApiController.uploadImport"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """""" + "$" + """file<.+>""", """controllers.FrontendController.assetOrDefault(file:String)"""),
    Nil
  ).foldLeft(List.empty[(String,String,String)]) { (s,e) => e.asInstanceOf[Any] match {
    case r @ (_,_,_) => s :+ r.asInstanceOf[(String,String,String)]
    case l => s ++ l.asInstanceOf[List[(String,String,String)]]
  }}


  // @LINE:8
  private[this] lazy val controllers_FrontendController_index0_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix)))
  )
  private[this] lazy val controllers_FrontendController_index0_invoker = createInvoker(
    FrontendController_2.index(),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.FrontendController",
      "index",
      Nil,
      "GET",
      this.prefix + """""",
      """ Handle Requests to the Server's root, and supply the Angular SPA""",
      Seq()
    )
  )

  // @LINE:9
  private[this] lazy val controllers_HealthController_health1_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("health")))
  )
  private[this] lazy val controllers_HealthController_health1_invoker = createInvoker(
    HealthController_0.health,
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.HealthController",
      "health",
      Nil,
      "GET",
      this.prefix + """health""",
      """""",
      Seq()
    )
  )

  // @LINE:13
  private[this] lazy val controllers_ApiController_getFeatureToggles2_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/featureToggles")))
  )
  private[this] lazy val controllers_ApiController_getFeatureToggles2_invoker = createInvoker(
    ApiController_1.getFeatureToggles,
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "getFeatureToggles",
      Nil,
      "GET",
      this.prefix + """api/v1/featureToggles""",
      """ serve the API v1 Specification
 TODO search-input URL path partially "behind" solrIndexId path component and partially not""",
      Seq()
    )
  )

  // @LINE:14
  private[this] lazy val controllers_ApiController_listAllSolrIndeces3_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/solr-index")))
  )
  private[this] lazy val controllers_ApiController_listAllSolrIndeces3_invoker = createInvoker(
    ApiController_1.listAllSolrIndeces,
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "listAllSolrIndeces",
      Nil,
      "GET",
      this.prefix + """api/v1/solr-index""",
      """""",
      Seq()
    )
  )

  // @LINE:15
  private[this] lazy val controllers_ApiController_getSolrIndex4_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/solr-index/"), DynamicPart("solrIndexId", """[^/]+""",true)))
  )
  private[this] lazy val controllers_ApiController_getSolrIndex4_invoker = createInvoker(
    ApiController_1.getSolrIndex(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "getSolrIndex",
      Seq(classOf[String]),
      "GET",
      this.prefix + """api/v1/solr-index/""" + "$" + """solrIndexId<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:16
  private[this] lazy val controllers_ApiController_addNewSolrIndex5_route = Route("PUT",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/solr-index")))
  )
  private[this] lazy val controllers_ApiController_addNewSolrIndex5_invoker = createInvoker(
    ApiController_1.addNewSolrIndex,
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "addNewSolrIndex",
      Nil,
      "PUT",
      this.prefix + """api/v1/solr-index""",
      """""",
      Seq()
    )
  )

  // @LINE:17
  private[this] lazy val controllers_ApiController_deleteSolrIndex6_route = Route("DELETE",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/solr-index/"), DynamicPart("solrIndexId", """[^/]+""",true)))
  )
  private[this] lazy val controllers_ApiController_deleteSolrIndex6_invoker = createInvoker(
    ApiController_1.deleteSolrIndex(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "deleteSolrIndex",
      Seq(classOf[String]),
      "DELETE",
      this.prefix + """api/v1/solr-index/""" + "$" + """solrIndexId<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:18
  private[this] lazy val controllers_ApiController_listAllInputTags7_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/inputTags")))
  )
  private[this] lazy val controllers_ApiController_listAllInputTags7_invoker = createInvoker(
    ApiController_1.listAllInputTags,
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "listAllInputTags",
      Nil,
      "GET",
      this.prefix + """api/v1/inputTags""",
      """""",
      Seq()
    )
  )

  // @LINE:19
  private[this] lazy val controllers_ApiController_listAllSearchInputs8_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/"), DynamicPart("solrIndexId", """[^/]+""",true), StaticPart("/search-input")))
  )
  private[this] lazy val controllers_ApiController_listAllSearchInputs8_invoker = createInvoker(
    ApiController_1.listAllSearchInputs(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "listAllSearchInputs",
      Seq(classOf[String]),
      "GET",
      this.prefix + """api/v1/""" + "$" + """solrIndexId<[^/]+>/search-input""",
      """""",
      Seq()
    )
  )

  // @LINE:20
  private[this] lazy val controllers_ApiController_getDetailedSearchInput9_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/search-input/"), DynamicPart("searchInputId", """[^/]+""",true)))
  )
  private[this] lazy val controllers_ApiController_getDetailedSearchInput9_invoker = createInvoker(
    ApiController_1.getDetailedSearchInput(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "getDetailedSearchInput",
      Seq(classOf[String]),
      "GET",
      this.prefix + """api/v1/search-input/""" + "$" + """searchInputId<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:21
  private[this] lazy val controllers_ApiController_addNewSearchInput10_route = Route("PUT",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/"), DynamicPart("solrIndexId", """[^/]+""",true), StaticPart("/search-input")))
  )
  private[this] lazy val controllers_ApiController_addNewSearchInput10_invoker = createInvoker(
    ApiController_1.addNewSearchInput(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "addNewSearchInput",
      Seq(classOf[String]),
      "PUT",
      this.prefix + """api/v1/""" + "$" + """solrIndexId<[^/]+>/search-input""",
      """""",
      Seq()
    )
  )

  // @LINE:22
  private[this] lazy val controllers_ApiController_updateSearchInput11_route = Route("POST",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/search-input/"), DynamicPart("searchInputId", """[^/]+""",true)))
  )
  private[this] lazy val controllers_ApiController_updateSearchInput11_invoker = createInvoker(
    ApiController_1.updateSearchInput(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "updateSearchInput",
      Seq(classOf[String]),
      "POST",
      this.prefix + """api/v1/search-input/""" + "$" + """searchInputId<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:23
  private[this] lazy val controllers_ApiController_deleteSearchInput12_route = Route("DELETE",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/search-input/"), DynamicPart("searchInputId", """[^/]+""",true)))
  )
  private[this] lazy val controllers_ApiController_deleteSearchInput12_invoker = createInvoker(
    ApiController_1.deleteSearchInput(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "deleteSearchInput",
      Seq(classOf[String]),
      "DELETE",
      this.prefix + """api/v1/search-input/""" + "$" + """searchInputId<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:24
  private[this] lazy val controllers_ApiController_updateRulesTxtForSolrIndexAndTargetPlatform13_route = Route("POST",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/"), DynamicPart("solrIndexId", """[^/]+""",true), StaticPart("/rules-txt/"), DynamicPart("targetPlatform", """[^/]+""",true)))
  )
  private[this] lazy val controllers_ApiController_updateRulesTxtForSolrIndexAndTargetPlatform13_invoker = createInvoker(
    ApiController_1.updateRulesTxtForSolrIndexAndTargetPlatform(fakeValue[String], fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "updateRulesTxtForSolrIndexAndTargetPlatform",
      Seq(classOf[String], classOf[String]),
      "POST",
      this.prefix + """api/v1/""" + "$" + """solrIndexId<[^/]+>/rules-txt/""" + "$" + """targetPlatform<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:25
  private[this] lazy val controllers_ApiController_listAllSuggestedSolrFields14_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/"), DynamicPart("solrIndexId", """[^/]+""",true), StaticPart("/suggested-solr-field")))
  )
  private[this] lazy val controllers_ApiController_listAllSuggestedSolrFields14_invoker = createInvoker(
    ApiController_1.listAllSuggestedSolrFields(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "listAllSuggestedSolrFields",
      Seq(classOf[String]),
      "GET",
      this.prefix + """api/v1/""" + "$" + """solrIndexId<[^/]+>/suggested-solr-field""",
      """""",
      Seq()
    )
  )

  // @LINE:26
  private[this] lazy val controllers_ApiController_addNewSuggestedSolrField15_route = Route("PUT",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/"), DynamicPart("solrIndexId", """[^/]+""",true), StaticPart("/suggested-solr-field")))
  )
  private[this] lazy val controllers_ApiController_addNewSuggestedSolrField15_invoker = createInvoker(
    ApiController_1.addNewSuggestedSolrField(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "addNewSuggestedSolrField",
      Seq(classOf[String]),
      "PUT",
      this.prefix + """api/v1/""" + "$" + """solrIndexId<[^/]+>/suggested-solr-field""",
      """""",
      Seq()
    )
  )

  // @LINE:27
  private[this] lazy val controllers_ApiController_deleteSuggestedSolrField16_route = Route("DELETE",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/"), DynamicPart("solrIndexId", """[^/]+""",true), StaticPart("/suggested-solr-field/"), DynamicPart("suggestedFieldId", """[^/]+""",true)))
  )
  private[this] lazy val controllers_ApiController_deleteSuggestedSolrField16_invoker = createInvoker(
    ApiController_1.deleteSuggestedSolrField(fakeValue[String], fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "deleteSuggestedSolrField",
      Seq(classOf[String], classOf[String]),
      "DELETE",
      this.prefix + """api/v1/""" + "$" + """solrIndexId<[^/]+>/suggested-solr-field/""" + "$" + """suggestedFieldId<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:28
  private[this] lazy val controllers_ApiController_downloadAllRulesTxtFiles17_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/allRulesTxtFiles")))
  )
  private[this] lazy val controllers_ApiController_downloadAllRulesTxtFiles17_invoker = createInvoker(
    ApiController_1.downloadAllRulesTxtFiles,
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "downloadAllRulesTxtFiles",
      Nil,
      "GET",
      this.prefix + """api/v1/allRulesTxtFiles""",
      """""",
      Seq()
    )
  )

  // @LINE:29
  private[this] lazy val controllers_ApiController_importFromRulesTxt18_route = Route("POST",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/"), DynamicPart("solrIndexId", """[^/]+""",true), StaticPart("/import-from-rules-txt")))
  )
  private[this] lazy val controllers_ApiController_importFromRulesTxt18_invoker = createInvoker(
    ApiController_1.importFromRulesTxt(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "importFromRulesTxt",
      Seq(classOf[String]),
      "POST",
      this.prefix + """api/v1/""" + "$" + """solrIndexId<[^/]+>/import-from-rules-txt""",
      """""",
      Seq()
    )
  )

  // @LINE:30
  private[this] lazy val controllers_ApiController_getLatestDeploymentResult19_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/log/deployment-info")))
  )
  private[this] lazy val controllers_ApiController_getLatestDeploymentResult19_invoker = createInvoker(
    ApiController_1.getLatestDeploymentResult(fakeValue[String], fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "getLatestDeploymentResult",
      Seq(classOf[String], classOf[String]),
      "GET",
      this.prefix + """api/v1/log/deployment-info""",
      """""",
      Seq()
    )
  )

  // @LINE:31
  private[this] lazy val controllers_ApiController_listAll20_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/"), DynamicPart("solrIndexId", """[^/]+""",true), StaticPart("/rules-and-spellings")))
  )
  private[this] lazy val controllers_ApiController_listAll20_invoker = createInvoker(
    ApiController_1.listAll(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "listAll",
      Seq(classOf[String]),
      "GET",
      this.prefix + """api/v1/""" + "$" + """solrIndexId<[^/]+>/rules-and-spellings""",
      """""",
      Seq()
    )
  )

  // @LINE:32
  private[this] lazy val controllers_ApiController_addNewSpelling21_route = Route("PUT",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/"), DynamicPart("solrIndexId", """[^/]+""",true), StaticPart("/spelling")))
  )
  private[this] lazy val controllers_ApiController_addNewSpelling21_invoker = createInvoker(
    ApiController_1.addNewSpelling(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "addNewSpelling",
      Seq(classOf[String]),
      "PUT",
      this.prefix + """api/v1/""" + "$" + """solrIndexId<[^/]+>/spelling""",
      """""",
      Seq()
    )
  )

  // @LINE:33
  private[this] lazy val controllers_ApiController_updateSpelling22_route = Route("POST",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/"), DynamicPart("solrIndexId", """[^/]+""",true), StaticPart("/spelling/"), DynamicPart("canonicalSpellingId", """[^/]+""",true)))
  )
  private[this] lazy val controllers_ApiController_updateSpelling22_invoker = createInvoker(
    ApiController_1.updateSpelling(fakeValue[String], fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "updateSpelling",
      Seq(classOf[String], classOf[String]),
      "POST",
      this.prefix + """api/v1/""" + "$" + """solrIndexId<[^/]+>/spelling/""" + "$" + """canonicalSpellingId<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:34
  private[this] lazy val controllers_ApiController_deleteSpelling23_route = Route("DELETE",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/spelling/"), DynamicPart("canonicalSpellingId", """[^/]+""",true)))
  )
  private[this] lazy val controllers_ApiController_deleteSpelling23_invoker = createInvoker(
    ApiController_1.deleteSpelling(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "deleteSpelling",
      Seq(classOf[String]),
      "DELETE",
      this.prefix + """api/v1/spelling/""" + "$" + """canonicalSpellingId<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:35
  private[this] lazy val controllers_ApiController_getDetailedSpelling24_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/spelling/"), DynamicPart("canonicalSpellingId", """[^/]+""",true)))
  )
  private[this] lazy val controllers_ApiController_getDetailedSpelling24_invoker = createInvoker(
    ApiController_1.getDetailedSpelling(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "getDetailedSpelling",
      Seq(classOf[String]),
      "GET",
      this.prefix + """api/v1/spelling/""" + "$" + """canonicalSpellingId<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:36
  private[this] lazy val controllers_ApiController_getActivityLog25_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/log/rule-activity-log")))
  )
  private[this] lazy val controllers_ApiController_getActivityLog25_invoker = createInvoker(
    ApiController_1.getActivityLog(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "getActivityLog",
      Seq(classOf[String]),
      "GET",
      this.prefix + """api/v1/log/rule-activity-log""",
      """""",
      Seq()
    )
  )

  // @LINE:37
  private[this] lazy val controllers_ApiController_getRulesReport26_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/report/rules-report/"), DynamicPart("solrIndexId", """[^/]+""",true)))
  )
  private[this] lazy val controllers_ApiController_getRulesReport26_invoker = createInvoker(
    ApiController_1.getRulesReport(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "getRulesReport",
      Seq(classOf[String]),
      "GET",
      this.prefix + """api/v1/report/rules-report/""" + "$" + """solrIndexId<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:38
  private[this] lazy val controllers_ApiController_getActivityReport27_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/report/activity-report/"), DynamicPart("solrIndexId", """[^/]+""",true)))
  )
  private[this] lazy val controllers_ApiController_getActivityReport27_invoker = createInvoker(
    ApiController_1.getActivityReport(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "getActivityReport",
      Seq(classOf[String]),
      "GET",
      this.prefix + """api/v1/report/activity-report/""" + "$" + """solrIndexId<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:39
  private[this] lazy val controllers_ApiController_getLatestVersionInfo28_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/version/latest-info")))
  )
  private[this] lazy val controllers_ApiController_getLatestVersionInfo28_invoker = createInvoker(
    ApiController_1.getLatestVersionInfo(),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "getLatestVersionInfo",
      Nil,
      "GET",
      this.prefix + """api/v1/version/latest-info""",
      """""",
      Seq()
    )
  )

  // @LINE:40
  private[this] lazy val controllers_ApiController_getDatabaseJsonWithId29_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/get-export-with-id/"), DynamicPart("id", """[^/]+""",true)))
  )
  private[this] lazy val controllers_ApiController_getDatabaseJsonWithId29_invoker = createInvoker(
    ApiController_1.getDatabaseJsonWithId(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "getDatabaseJsonWithId",
      Seq(classOf[String]),
      "GET",
      this.prefix + """api/v1/get-export-with-id/""" + "$" + """id<[^/]+>""",
      """""",
      Seq()
    )
  )

  // @LINE:41
  private[this] lazy val controllers_ApiController_uploadImport30_route = Route("POST",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/v1/upload-import")))
  )
  private[this] lazy val controllers_ApiController_uploadImport30_invoker = createInvoker(
    ApiController_1.uploadImport,
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.ApiController",
      "uploadImport",
      Nil,
      "POST",
      this.prefix + """api/v1/upload-import""",
      """""",
      Seq()
    )
  )

  // @LINE:44
  private[this] lazy val controllers_FrontendController_assetOrDefault31_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), DynamicPart("file", """.+""",false)))
  )
  private[this] lazy val controllers_FrontendController_assetOrDefault31_invoker = createInvoker(
    FrontendController_2.assetOrDefault(fakeValue[String]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "controllers.FrontendController",
      "assetOrDefault",
      Seq(classOf[String]),
      "GET",
      this.prefix + """""" + "$" + """file<.+>""",
      """ Map static resources from the /public folder to the /assets URL path""",
      Seq()
    )
  )


  def routes: PartialFunction[RequestHeader, Handler] = {
  
    // @LINE:8
    case controllers_FrontendController_index0_route(params@_) =>
      call { 
        controllers_FrontendController_index0_invoker.call(FrontendController_2.index())
      }
  
    // @LINE:9
    case controllers_HealthController_health1_route(params@_) =>
      call { 
        controllers_HealthController_health1_invoker.call(HealthController_0.health)
      }
  
    // @LINE:13
    case controllers_ApiController_getFeatureToggles2_route(params@_) =>
      call { 
        controllers_ApiController_getFeatureToggles2_invoker.call(ApiController_1.getFeatureToggles)
      }
  
    // @LINE:14
    case controllers_ApiController_listAllSolrIndeces3_route(params@_) =>
      call { 
        controllers_ApiController_listAllSolrIndeces3_invoker.call(ApiController_1.listAllSolrIndeces)
      }
  
    // @LINE:15
    case controllers_ApiController_getSolrIndex4_route(params@_) =>
      call(params.fromPath[String]("solrIndexId", None)) { (solrIndexId) =>
        controllers_ApiController_getSolrIndex4_invoker.call(ApiController_1.getSolrIndex(solrIndexId))
      }
  
    // @LINE:16
    case controllers_ApiController_addNewSolrIndex5_route(params@_) =>
      call { 
        controllers_ApiController_addNewSolrIndex5_invoker.call(ApiController_1.addNewSolrIndex)
      }
  
    // @LINE:17
    case controllers_ApiController_deleteSolrIndex6_route(params@_) =>
      call(params.fromPath[String]("solrIndexId", None)) { (solrIndexId) =>
        controllers_ApiController_deleteSolrIndex6_invoker.call(ApiController_1.deleteSolrIndex(solrIndexId))
      }
  
    // @LINE:18
    case controllers_ApiController_listAllInputTags7_route(params@_) =>
      call { 
        controllers_ApiController_listAllInputTags7_invoker.call(ApiController_1.listAllInputTags)
      }
  
    // @LINE:19
    case controllers_ApiController_listAllSearchInputs8_route(params@_) =>
      call(params.fromPath[String]("solrIndexId", None)) { (solrIndexId) =>
        controllers_ApiController_listAllSearchInputs8_invoker.call(ApiController_1.listAllSearchInputs(solrIndexId))
      }
  
    // @LINE:20
    case controllers_ApiController_getDetailedSearchInput9_route(params@_) =>
      call(params.fromPath[String]("searchInputId", None)) { (searchInputId) =>
        controllers_ApiController_getDetailedSearchInput9_invoker.call(ApiController_1.getDetailedSearchInput(searchInputId))
      }
  
    // @LINE:21
    case controllers_ApiController_addNewSearchInput10_route(params@_) =>
      call(params.fromPath[String]("solrIndexId", None)) { (solrIndexId) =>
        controllers_ApiController_addNewSearchInput10_invoker.call(ApiController_1.addNewSearchInput(solrIndexId))
      }
  
    // @LINE:22
    case controllers_ApiController_updateSearchInput11_route(params@_) =>
      call(params.fromPath[String]("searchInputId", None)) { (searchInputId) =>
        controllers_ApiController_updateSearchInput11_invoker.call(ApiController_1.updateSearchInput(searchInputId))
      }
  
    // @LINE:23
    case controllers_ApiController_deleteSearchInput12_route(params@_) =>
      call(params.fromPath[String]("searchInputId", None)) { (searchInputId) =>
        controllers_ApiController_deleteSearchInput12_invoker.call(ApiController_1.deleteSearchInput(searchInputId))
      }
  
    // @LINE:24
    case controllers_ApiController_updateRulesTxtForSolrIndexAndTargetPlatform13_route(params@_) =>
      call(params.fromPath[String]("solrIndexId", None), params.fromPath[String]("targetPlatform", None)) { (solrIndexId, targetPlatform) =>
        controllers_ApiController_updateRulesTxtForSolrIndexAndTargetPlatform13_invoker.call(ApiController_1.updateRulesTxtForSolrIndexAndTargetPlatform(solrIndexId, targetPlatform))
      }
  
    // @LINE:25
    case controllers_ApiController_listAllSuggestedSolrFields14_route(params@_) =>
      call(params.fromPath[String]("solrIndexId", None)) { (solrIndexId) =>
        controllers_ApiController_listAllSuggestedSolrFields14_invoker.call(ApiController_1.listAllSuggestedSolrFields(solrIndexId))
      }
  
    // @LINE:26
    case controllers_ApiController_addNewSuggestedSolrField15_route(params@_) =>
      call(params.fromPath[String]("solrIndexId", None)) { (solrIndexId) =>
        controllers_ApiController_addNewSuggestedSolrField15_invoker.call(ApiController_1.addNewSuggestedSolrField(solrIndexId))
      }
  
    // @LINE:27
    case controllers_ApiController_deleteSuggestedSolrField16_route(params@_) =>
      call(params.fromPath[String]("solrIndexId", None), params.fromPath[String]("suggestedFieldId", None)) { (solrIndexId, suggestedFieldId) =>
        controllers_ApiController_deleteSuggestedSolrField16_invoker.call(ApiController_1.deleteSuggestedSolrField(solrIndexId, suggestedFieldId))
      }
  
    // @LINE:28
    case controllers_ApiController_downloadAllRulesTxtFiles17_route(params@_) =>
      call { 
        controllers_ApiController_downloadAllRulesTxtFiles17_invoker.call(ApiController_1.downloadAllRulesTxtFiles)
      }
  
    // @LINE:29
    case controllers_ApiController_importFromRulesTxt18_route(params@_) =>
      call(params.fromPath[String]("solrIndexId", None)) { (solrIndexId) =>
        controllers_ApiController_importFromRulesTxt18_invoker.call(ApiController_1.importFromRulesTxt(solrIndexId))
      }
  
    // @LINE:30
    case controllers_ApiController_getLatestDeploymentResult19_route(params@_) =>
      call(params.fromQuery[String]("solrIndexId", None), params.fromQuery[String]("targetSystem", None)) { (solrIndexId, targetSystem) =>
        controllers_ApiController_getLatestDeploymentResult19_invoker.call(ApiController_1.getLatestDeploymentResult(solrIndexId, targetSystem))
      }
  
    // @LINE:31
    case controllers_ApiController_listAll20_route(params@_) =>
      call(params.fromPath[String]("solrIndexId", None)) { (solrIndexId) =>
        controllers_ApiController_listAll20_invoker.call(ApiController_1.listAll(solrIndexId))
      }
  
    // @LINE:32
    case controllers_ApiController_addNewSpelling21_route(params@_) =>
      call(params.fromPath[String]("solrIndexId", None)) { (solrIndexId) =>
        controllers_ApiController_addNewSpelling21_invoker.call(ApiController_1.addNewSpelling(solrIndexId))
      }
  
    // @LINE:33
    case controllers_ApiController_updateSpelling22_route(params@_) =>
      call(params.fromPath[String]("solrIndexId", None), params.fromPath[String]("canonicalSpellingId", None)) { (solrIndexId, canonicalSpellingId) =>
        controllers_ApiController_updateSpelling22_invoker.call(ApiController_1.updateSpelling(solrIndexId, canonicalSpellingId))
      }
  
    // @LINE:34
    case controllers_ApiController_deleteSpelling23_route(params@_) =>
      call(params.fromPath[String]("canonicalSpellingId", None)) { (canonicalSpellingId) =>
        controllers_ApiController_deleteSpelling23_invoker.call(ApiController_1.deleteSpelling(canonicalSpellingId))
      }
  
    // @LINE:35
    case controllers_ApiController_getDetailedSpelling24_route(params@_) =>
      call(params.fromPath[String]("canonicalSpellingId", None)) { (canonicalSpellingId) =>
        controllers_ApiController_getDetailedSpelling24_invoker.call(ApiController_1.getDetailedSpelling(canonicalSpellingId))
      }
  
    // @LINE:36
    case controllers_ApiController_getActivityLog25_route(params@_) =>
      call(params.fromQuery[String]("inputId", None)) { (inputId) =>
        controllers_ApiController_getActivityLog25_invoker.call(ApiController_1.getActivityLog(inputId))
      }
  
    // @LINE:37
    case controllers_ApiController_getRulesReport26_route(params@_) =>
      call(params.fromPath[String]("solrIndexId", None)) { (solrIndexId) =>
        controllers_ApiController_getRulesReport26_invoker.call(ApiController_1.getRulesReport(solrIndexId))
      }
  
    // @LINE:38
    case controllers_ApiController_getActivityReport27_route(params@_) =>
      call(params.fromPath[String]("solrIndexId", None)) { (solrIndexId) =>
        controllers_ApiController_getActivityReport27_invoker.call(ApiController_1.getActivityReport(solrIndexId))
      }
  
    // @LINE:39
    case controllers_ApiController_getLatestVersionInfo28_route(params@_) =>
      call { 
        controllers_ApiController_getLatestVersionInfo28_invoker.call(ApiController_1.getLatestVersionInfo())
      }
  
    // @LINE:40
    case controllers_ApiController_getDatabaseJsonWithId29_route(params@_) =>
      call(params.fromPath[String]("id", None)) { (id) =>
        controllers_ApiController_getDatabaseJsonWithId29_invoker.call(ApiController_1.getDatabaseJsonWithId(id))
      }
  
    // @LINE:41
    case controllers_ApiController_uploadImport30_route(params@_) =>
      call { 
        controllers_ApiController_uploadImport30_invoker.call(ApiController_1.uploadImport)
      }
  
    // @LINE:44
    case controllers_FrontendController_assetOrDefault31_route(params@_) =>
      call(params.fromPath[String]("file", None)) { (file) =>
        controllers_FrontendController_assetOrDefault31_invoker.call(FrontendController_2.assetOrDefault(file))
      }
  }
}
