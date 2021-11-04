package services

import java.time.LocalDateTime
import javax.inject.Inject
import util.control.Breaks._
import play.api.Logging
import play.api.libs.json.{Json, JsArray, JsObject, JsValue}

import scala.collection.mutable.ListBuffer
import models._
import models.rules._
import models.input.{InputTag, SearchInputId, SearchInputWithRules}
import models.querqy.QuerqyRulesTxtGenerator
import models.FeatureToggleModel.FeatureToggleService

@javax.inject.Singleton
class RulesTxtImportService @Inject() (querqyRulesTxtGenerator: QuerqyRulesTxtGenerator,
                                       searchManagementRepository: SearchManagementRepository,
                                       featureToggleService: FeatureToggleService) extends Logging {

  trait PreliminaryRule {
    def term: String
  }
  case class PreliminarySynonymRule(term: String, var synonymType: Int = SynonymRule.TYPE_DIRECTED) extends PreliminaryRule
  case class PreliminaryUpDownRule(term: String, upDownType: Int, boostMalusValue: Int) extends PreliminaryRule
  case class PreliminaryFilterRule(term: String) extends PreliminaryRule
  case class PreliminaryDeleteRule(term: String) extends PreliminaryRule
  case class PreliminarySearchInput(term: String, var rules: List[PreliminaryRule], var jsonTags: Option[JsObject] = None, var allSynonymTermHash: String = null, var remainingRulesHash: String = null, var allTagsHash: String = null)

  // These tags are never imported. See querqy.rewrite.commonrules.model.Instructions.StandardPropertyNames
  val IGNORED_TAG_ID = "_id"
  val IGNORED_TAG_LOG = "_log"

  def importFromFilePayload(filePayload: String, solrIndexId: SolrIndexId): (Int, Int, Int, Int, Int) = {
    println("In RulesTxtImportService.importFromFilePayload")
    println(":: filePayload = " + filePayload.getBytes().length +  " bytes")
    println(":: solrIndexId = " + solrIndexId)
    var isRuleTaggingActive = featureToggleService.isRuleTaggingActive
    var allowOnlyPredefinedTags = !featureToggleService.predefinedTagsFileName.isEmpty
    println(":: isRuleTaggingActive = " + isRuleTaggingActive + " (true = Tags are imported)")
    println(":: allowOnlyPredefinedTags = " + allowOnlyPredefinedTags + " (true = Only predefined tags are allowed)")


    // Parse with Querqy to ensure valid syntax or throw exception
    val validationErrorMessage = querqyRulesTxtGenerator.validateQuerqyRulesTxtToErrMsg(filePayload)
    if (validationErrorMessage.isDefined) {
      throw new IllegalArgumentException(validationErrorMessage.get)
    }

    // PARSE
    // engineer rules data structure from rules.txt file payload
    val rulesTxtModel = ListBuffer.empty[PreliminarySearchInput]
    var currInput: PreliminarySearchInput = null
    var currRules: ListBuffer[PreliminaryRule] = null
    var currentlyReadingMultilineTagLines: Boolean = false
    var currSingleLineTagsJson: String = ""
    var currMultiLineTagsJson: String = ""
    var retstatCountRulesTxtLinesSkipped = 0
    var retstatCountRulesTxtUnkownConvert = 0
    for(ruleLine: String <- filePayload.split("\n")) {

      // if line is empty or contains a comment, skip
      val b_lineEmptyOrComment = (ruleLine.trim().length() < 1) || (ruleLine.trim().startsWith("#"))

      if(!b_lineEmptyOrComment) {
        // match pattern for search input
        // TODO make robust against input is not first syntax element in line orders

        "(.*?) =>".r.findFirstMatchIn(ruleLine.trim()) match {
          case Some(m) => {
            commitCollectedRulesAndTags(currRules, currSingleLineTagsJson, currMultiLineTagsJson, currInput)

            // Reset to start fresh
            currRules = ListBuffer.empty[PreliminaryRule]
            currSingleLineTagsJson = "" // reset
            currMultiLineTagsJson = "" // reset
            currInput = new PreliminarySearchInput(m.group(1), List.empty[PreliminaryRule])
            rulesTxtModel += currInput
          }
          case None => {
            // match pattern for synonyms (directed-only assumed)
            "^[\\s]*?SYNONYM: (.*)".r.findFirstMatchIn(ruleLine.trim()) match {
              case Some(m) => {
                currRules += PreliminarySynonymRule(m.group(1))
              }
              case None => {
                // match pattern for UP/DOWN
                "^[\\s]*?(UP|DOWN)\\((\\d*)\\): (.*)".r.findFirstMatchIn(ruleLine.trim()) match {
                  case Some(m) => {
                    // TODO make robust against, neither "UP" nor "DOWN" appeared
                    var upDownType = UpDownRule.TYPE_UP
                    if(m.group(1).trim() == "DOWN") {
                      upDownType = UpDownRule.TYPE_DOWN
                    }
                    // TODO make robust against string not convertable to integer
                    val boostMalusValue = m.group(2).trim().toInt
                    currRules += PreliminaryUpDownRule(m.group(3), upDownType, boostMalusValue)
                  }
                  case None => {
                    // match pattern for FILTER
                    "^[\\s]*?FILTER: (.*)".r.findFirstMatchIn(ruleLine.trim()) match {
                      case Some(m) => {
                        currRules += PreliminaryFilterRule(m.group(1))
                      }
                      case None => {
                        // match pattern for DELETE
                        "^[\\s]*?DELETE: (.*)".r.findFirstMatchIn(ruleLine.trim()) match {
                          case Some(m) => {
                            currRules += PreliminaryDeleteRule(m.group(1))
                          }
                          case None => {

                            def countUnknownConvert = {
                              // unknown, if reached that point
                              println("Cannot convert line >>>" + ruleLine)
                              retstatCountRulesTxtUnkownConvert += 1
                            }

                            if (isRuleTaggingActive) {
                              // match pattern for single line @-Tags "@..."
                              "^[\\s]*?@[^\\{](.*)".r.findFirstMatchIn(ruleLine.trim()) match {
                                case Some(m) => {
                                  val s = ruleLine.trim().substring(1) //remove '@'
                                  var comma = ""
                                  if (currSingleLineTagsJson != "") comma = ","
                                  currSingleLineTagsJson += comma + s
                                }
                                case None => {

                                  // match pattern for start of multi line @-Tags "@{ ... }@
                                  "^[\\s]*?(@\\{)".r.findFirstMatchIn(ruleLine.trim()) match {
                                    case Some(m) => {
                                      //start @ for tags
                                      val s = ruleLine.trim()
                                      if (s.endsWith("}@")) { // start and end on the same line
                                        currMultiLineTagsJson += s.substring(2, s.length-2) //remove '@{' & '}@'
                                      } else { // only start on the same line
                                        currMultiLineTagsJson += s.substring(2) //remove '@{'
                                        currentlyReadingMultilineTagLines = true
                                      }

                                    }
                                    case None => {
                                      if (currentlyReadingMultilineTagLines) {
                                        // match pattern for end of multi line @-Tags "@{ ... }@"
                                        "^[\\s]*?(\\}@)$".r.findFirstMatchIn(ruleLine.trim()) match {
                                          case Some(m) => {
                                            // end @ for tags
                                            currentlyReadingMultilineTagLines = false
                                          }
                                          case None => {
                                            //reading multi line @-Tags
                                            currMultiLineTagsJson += ruleLine.trim()
                                          }
                                        }

                                      } else {
                                        countUnknownConvert
                                      }
                                    }
                                  }
                                }
                              }
                            } else {
                              countUnknownConvert
                            }

                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      } else {
        retstatCountRulesTxtLinesSkipped += 1
      }
    }
    // commit last rules & tags
    commitCollectedRulesAndTags(currRules, currSingleLineTagsJson, currMultiLineTagsJson, currInput)

    def commitCollectedRulesAndTags(currRules: ListBuffer[PreliminaryRule], currSingleLineTagsJson: String, currMultiLineTagsJson: String, currInput: PreliminarySearchInput): Unit = {
      if (currRules != null) {
        currInput.rules = currRules.clone().toList
      }
      if (currSingleLineTagsJson != "" || currMultiLineTagsJson != "") {
        // commit collected tags to curr_input & empty
        var comma = ","
        if (currSingleLineTagsJson == "" || currMultiLineTagsJson == "") comma = ""
        val allJsonTags = "{" + currSingleLineTagsJson + comma + currMultiLineTagsJson + "}"
        currInput.jsonTags = Some(Json.parse(allJsonTags).as[JsObject] - IGNORED_TAG_ID - IGNORED_TAG_LOG)
      }
    }

    // print some stats
    println("retstatCountRulesTxtLinesSkipped = " + retstatCountRulesTxtLinesSkipped)
    println("retstatCountRulesTxtUnkownConvert = " + retstatCountRulesTxtUnkownConvert)

    // POST PROCESS
    // annotate rulesTxtModel with fingerprints related to synonym/other rules
    def synonymFingerprint(input: PreliminarySearchInput): String = {
      val termsOfInput = input.term ::
        input.rules.filter(_.isInstanceOf[PreliminarySynonymRule])
          .map(_.term)
      val sortedTermsOfInput = termsOfInput.sorted
      return sortedTermsOfInput.mkString("")
    }
    def otherRulesFingerprint(input: PreliminarySearchInput): String = {
      val remainingRules = input.rules.filter(!_.isInstanceOf[PreliminarySynonymRule])
        .sortBy(_.term)
      if(remainingRules.size < 1) {
        return "0"
      } else {
        return remainingRules.hashCode().toString
      }
    }
    def tagsFingerprint(input: PreliminarySearchInput): String = {
      //Sorting is necessary because play.api.libs.json considers JsArray with same elements but different ordering as not equal. (behaviour tested up to play version 2.8.8)
      def sortArrays(json: JsValue): JsValue = json match {
        case JsObject(obj) => JsObject(obj.toMap.mapValues(sortArrays(_)).toList)
        case JsArray(arr) => JsArray(arr.map(sortArrays).sortBy(_.toString))
        case other => other
      }

      if (input.jsonTags.isDefined) {
        return sortArrays(input.jsonTags.get).hashCode().toString
      } else {
        return "0"
      }

    }

    for(input <- rulesTxtModel) {
      input.allSynonymTermHash = synonymFingerprint(input)
      input.remainingRulesHash = otherRulesFingerprint(input)
      input.allTagsHash = tagsFingerprint(input)
    }
    // start collecting retstat (= return statistics)
    val retstatCountRulesTxtInputs = rulesTxtModel.size
    println("retstatCountRulesTxtInputs = " + retstatCountRulesTxtInputs)
    // consolidate to target SMUI model data structure (whilst identifying undirected synonyms)
    val importPrelimSearchInputs = ListBuffer.empty[PreliminarySearchInput]
    val skip_i = ListBuffer.empty[Int] // indices of inputs & rules skipped, because they are collapsed as an undirected synonym
    for((a_input, i) <- rulesTxtModel.zipWithIndex) {
      if(!skip_i.toList.contains(i)) {
        for((b_input, j) <- rulesTxtModel.zipWithIndex) {
          if(i != j) {
            if((a_input.allSynonymTermHash == b_input.allSynonymTermHash) &&
               (a_input.remainingRulesHash == b_input.remainingRulesHash) &&
               (a_input.allTagsHash == b_input.allTagsHash)) {
              println("Found matching undirected synonym on term = " + a_input.term)
              // find matching synonym rule in a_input
              for(a_synonymRule <- a_input.rules.filter(_.isInstanceOf[PreliminarySynonymRule])) breakable {
                if(a_synonymRule.term == b_input.term) {
                  println("^-- Found according synonym on " + b_input.term + " in = " + a_synonymRule)
                  a_synonymRule.asInstanceOf[PreliminarySynonymRule].synonymType = SynonymRule.TYPE_UNDIRECTED
                  break
                }
              }
              skip_i += j
            }
          }
        }
        importPrelimSearchInputs += a_input
      }
    }
    val retstatCountConsolidatedInputs = importPrelimSearchInputs.size
    println("retstatCountConsolidatedInputs = " + retstatCountConsolidatedInputs)
    val retstatCountConsolidatedRules = importPrelimSearchInputs
      .toList
      .foldLeft(0) {
        (s, i) => s + i.rules.size
      }
    println("retstatCountConsolidatedRules = " + retstatCountConsolidatedRules)

    // IMPORT INTO DB
    var predefinedInputTags: Seq[InputTag] = searchManagementRepository.listAllInputTags()
    var newlyCreatedInputTags = new ListBuffer[InputTag]() // to be imported into the DB

    // convert
    def preliminaryToFinalInput(preliminarySearchInput: PreliminarySearchInput): SearchInputWithRules = {

      val now = LocalDateTime.now()
      // define converter
      def preliminaryToFinalSynonymRule(preliminarySynonymRule: PreliminarySynonymRule): SynonymRule = {
        return new SynonymRule(
          SynonymRuleId(),
          preliminarySynonymRule.synonymType,
          preliminarySynonymRule.term,
          true
        )
      }
      def preliminaryToFinalUpDownRule(preliminaryUpDownRule: PreliminaryUpDownRule): UpDownRule = {
        return new UpDownRule(
          UpDownRuleId(),
          preliminaryUpDownRule.upDownType,
          preliminaryUpDownRule.boostMalusValue,
          preliminaryUpDownRule.term,
          true
        )
      }
      def preliminaryToFinalFilterRule(preliminaryFilterRule: PreliminaryFilterRule): FilterRule = {
        return new FilterRule(
          FilterRuleId(),
          preliminaryFilterRule.term,
          true
        )
      }
      def preliminaryToFinalDeleteRule(preliminaryDeleteRule: PreliminaryDeleteRule): DeleteRule = {
        return new DeleteRule(
          DeleteRuleId(),
          preliminaryDeleteRule.term,
          true
        )
      }
      def preliminaryTagToFinalInputTags(preliminaryJsonTags: JsObject, predefinedInputTags: Seq[InputTag]): List[InputTag] = {

        def findMatchingPredefinedInputTag(key: String, value: String, availableInputTags: Seq[InputTag]): Option[InputTag] = {
          //println("Looking for a matching predefined Tag: " + key + " : " + value)
          for (availableInputTag <- availableInputTags) {
            val inputTagName = availableInputTag.property.getOrElse ("??")
            val inputTagValue = availableInputTag.value
            if (inputTagName == key && inputTagValue == value) {
              //println("Found matching predefined Tag: " + predefinedInputTag)
              return Some(availableInputTag)
            }
          }

          return None;
        }

        var inputTags = new ListBuffer[InputTag]()
        //println("Trying to match tags from imported Rule to predefined SMUI tags ...")

        // Try to match every name/value tag from JSON to predefined SMUI InputTag.
        for (key <- preliminaryJsonTags.keys) {
          val value = (preliminaryJsonTags \ key).get
          if (value.isInstanceOf[JsArray]) {
            for (item <- value.as[List[JsValue]]) {
              linkToExistingOrCreateNewTag(key, item, item.as[String], allowOnlyPredefinedTags)
            }
          } else {
            linkToExistingOrCreateNewTag(key, value, value.as[String], allowOnlyPredefinedTags)
          }

        }

        def linkToExistingOrCreateNewTag(key: String, item: JsValue, tagValue: String, doNotCreateNewTags: Boolean) = {
          val matchedInputTag = findMatchingPredefinedInputTag(key, item.as[String], predefinedInputTags)
          if (matchedInputTag.isDefined) {
            // re-use existing InputTag Definition
            inputTags += matchedInputTag.get
          } else {
            // No InputTag found
            if (doNotCreateNewTags) {
              throw new IllegalArgumentException("Unknown tag = " + key + " : " + tagValue)
            } else {
              val inputTag = InputTag.create(Some(solrIndexId), Some(key), tagValue, true, false)
              newlyCreatedInputTags += inputTag // to be added to the database before linking it with a newly added rule
              inputTags += inputTag // is used to be linked with the newly added rule later
              println("New InputTag created: " + key + ":" + tagValue)
            }
          }
        }

        return inputTags.toList
      }


      // convert rules
      val synonymRules = preliminarySearchInput
        .rules
        .filter(_.isInstanceOf[PreliminarySynonymRule])
        .map(r => preliminaryToFinalSynonymRule(r.asInstanceOf[PreliminarySynonymRule]))
      val upDownRules = preliminarySearchInput
        .rules
        .filter(_.isInstanceOf[PreliminaryUpDownRule])
        .map(r => preliminaryToFinalUpDownRule(r.asInstanceOf[PreliminaryUpDownRule]))
      val filterRules = preliminarySearchInput
        .rules
        .filter(_.isInstanceOf[PreliminaryFilterRule])
        .map(r => preliminaryToFinalFilterRule(r.asInstanceOf[PreliminaryFilterRule]))
      val deleteRules = preliminarySearchInput
        .rules
        .filter(_.isInstanceOf[PreliminaryDeleteRule])
        .map(r => preliminaryToFinalDeleteRule(r.asInstanceOf[PreliminaryDeleteRule]))
      val inputTags = if (preliminarySearchInput.jsonTags.isDefined)
        preliminaryTagToFinalInputTags(preliminarySearchInput.jsonTags.get, predefinedInputTags ++ newlyCreatedInputTags)
      else
        List()

      // convert final input (passing its rules)
      return new SearchInputWithRules(
        SearchInputId("--empty--"),
        preliminarySearchInput.term,
        synonymRules,
        upDownRules,
        filterRules,
        deleteRules,
        Nil,
        inputTags,
        true,
        "Added by rules.txt import." // TODO add a timestamp to comment.
      )
    }
    val finalInputs = importPrelimSearchInputs.map(preliminaryToFinalInput(_))
    println("finalInputs.size = " + finalInputs.size)
    def __DEBUG_count_all_rules(i: SearchInputWithRules): Int = {
      return i.synonymRules.size +
        i.upDownRules.size +
        i.filterRules.size +
        i.deleteRules.size
    }
    println("finalInputs :: total rules = " + finalInputs
      .toList
      .foldLeft(0) {
        (s, i) => s + __DEBUG_count_all_rules(i)
      }
    )

    // write to DB

    // start with inserting new tags so the SearchInputWithRules can link to them (because SearchInputWithRules.update() does not yet support creating new tags if desired)
    newlyCreatedInputTags.foreach { newInputTag =>
      searchManagementRepository.addNewInputTag(newInputTag)
    }

    finalInputs.foreach { searchInput =>
      def inputWithDbId(input: SearchInputWithRules, dbId: SearchInputId): SearchInputWithRules = {
        return new SearchInputWithRules(
          dbId,
          input.term,
          input.synonymRules,
          input.upDownRules,
          input.filterRules,
          input.deleteRules,
          input.redirectRules,
          input.tags,
          true,
          "Added by rules.txt import." // TODO add a timestamp to comment.
        )
      }
      // first create entity
      val tagIds = searchInput.tags.map(inputTag => inputTag.id)
      val searchInputId = searchManagementRepository.addNewSearchInput(
        solrIndexId, searchInput.term, tagIds, None
      )
      // then update (incl rules)
      searchManagementRepository.updateSearchInput(
        inputWithDbId(searchInput, searchInputId), None
      )
    }
    // return stats
    val retStatistics = (
      retstatCountRulesTxtInputs,
      retstatCountRulesTxtLinesSkipped,
      retstatCountRulesTxtUnkownConvert,
      retstatCountConsolidatedInputs,
      retstatCountConsolidatedRules
    )
    return retStatistics
  }

}
