package com.groupaxis.groupsuite.routing.write.domain.model.routing.rule

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{ FromRequestUnmarshaller, Unmarshaller, FromEntityUnmarshaller }
import akka.stream.Materializer
import akka.util.ByteString
import spray.json.{ DefaultJsonProtocol, _ }

import scala.concurrent.ExecutionContext
import scala.xml.{ Elem, XML, NodeSeq }
import scala.xml.Atom
import java.text.DateFormat
import java.util.Date
import java.text.SimpleDateFormat
import com.groupaxis.groupsuite.routing.write.domain.model.routing.schema.Schema
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleMessages.RuleCreated
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleMessages.RuleUpdated
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleMessages.RuleFound
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleMessages.RulesFound

object MediaVersionTypes {
  def customMediatype(subType: String) = MediaType.customWithFixedCharset("application", subType, HttpCharsets.`UTF-8`)
  val `application/groupsuite.routing.rule.v1+json` = customMediatype("groupsuite.routing.rule.v1+json")
  val `application/groupsuite.routing.rule.v1+xml` = customMediatype("groupsuite.routing.rule.v1+xml")
}

trait RuleMarshallers extends DefaultJsonProtocol with SprayJsonSupport with ScalaXmlSupport {
  implicit def ec: ExecutionContext;

  implicit object DateJsonFormat extends JsonFormat[Date] {

    private val dateParser: SimpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");

    def write(x: Date) = JsString(dateParser.format(x));

    def read(value: JsValue): Date = value match {
      case JsString(x) => dateParser.parse(x);
      case x           => deserializationError("Expected String as dd-MM-yyyy, but got " + x)
    }
  }

  implicit object ActionTypeFormat extends JsonFormat[ActionType] {
    def write(obj: ActionType) = JsString(obj.toString)

    def read(json: JsValue): ActionType = json match {
      case JsString("SourceType")               => SourceType
      case JsString("NewInstanceType")          => NewInstanceType
      case JsString("SourceAndNewInstanceType") => SourceAndNewInstanceType

    }
  }

  implicit object ConditionTypeFormat extends JsonFormat[ConditionType] {
    def write(obj: ConditionType) = JsString(obj.toString)

    def read(json: JsValue): ConditionType = json match {
      case JsString("MessageType")            => MessageType
      case JsString("FunctionType")           => FunctionType
      case JsString("MessageAndFunctionType") => MessageAndFunctionType

    }
  }

  implicit val sourceJsonFormatV1 = jsonFormat7(Source)
  implicit val newInstanceJsonFormatV1 = jsonFormat9(NewInstance)
  implicit val conditionFunctionJsonFormatV1 = jsonFormat2(ConditionFunction)
  implicit val conditionJsonFormatV1 = jsonFormat3(Condition)
  implicit val actionJsonFormatV1 = jsonFormat3(Action)
  implicit val schemaJsonFormatV1 = jsonFormat2(Schema)

  implicit val ruleJsonFormatV1 = jsonFormat9(Rule)

  /**
    * HELPERS
    */
  implicit def optionElem(e: Elem) = new {
    def ? : NodeSeq = {
      require(e.child.length == 1)
      e.child.head match {
        case atom: Atom[Option[_]] => atom.data match {
          case None => NodeSeq.Empty
          case Some(x) => e.copy(child = x match {
            case n: NodeSeq => n
            case x          => new Atom(x)
          })
        }
        case _ => e
      }
    }
  }
  /***************** XML SCHEMA ********************/
  def marshalSchemaXmlV1(schema: Schema): NodeSeq =
    <schema>
      <id> { schema.name } </id>
      { <description> { schema.description } </description> ? }
    </schema>

  def marshalSchemasXmlV1(schemas: Iterable[Schema]): NodeSeq =
    <schemas>
      { schemas.map(marshalSchemaXmlV1) }
    </schemas>

  implicit def schemasXmlFormatV1 = Marshaller.opaque[Iterable[Schema], NodeSeq](marshalSchemasXmlV1)

  implicit def schemaXmlFormatV1 = Marshaller.opaque[Schema, NodeSeq](marshalSchemaXmlV1)

  /***************** XML CONDITION FUNCTION ********************/
  def marshalFunctionXmlV1(function: ConditionFunction): NodeSeq =
    <function>
      <id> { function.id } </id>
      { <description> { function.description } </description> ? }
    </function>

  def marshalFunctionsXmlV1(functions: Iterable[ConditionFunction]): NodeSeq =
    <functions>
      { functions.map(marshalFunctionXmlV1) }
    </functions>

  implicit def functionsXmlFormatV1 = Marshaller.opaque[Iterable[ConditionFunction], NodeSeq](marshalFunctionsXmlV1)

  implicit def functionXmlFormatV1 = Marshaller.opaque[ConditionFunction, NodeSeq](marshalFunctionXmlV1)

  /***************** XML CONDITION ********************/
  def marshalConditionXmlV1(condition: Condition): NodeSeq =
    <condition>
      <condition-on> { condition.conditionOn } </condition-on>
      { condition.functions.fold(NodeSeq.Empty)(l => marshalFunctionsXmlV1(l)) }
      { <message> { condition.message } </message> ? }
    </condition>

  implicit def conditionXmlFormatV1 = Marshaller.opaque[Condition, NodeSeq](marshalConditionXmlV1)
  
  /***************** XML SOURCE ********************/
  def marshalSourceXmlV1(source: Source): NodeSeq =
    <source>
      <id> { source.actionId } </id>
      { <action-option-id> { source.actionOptionId } </action-option-id> ? }
      <intervention> { source.interventionId } </intervention>
      { <action-intervention-text> { source.interventionText } </action-intervention-text> ? }
      <unit-id> { source.unitId } </unit-id>
      { <routing-code> { source.routingCode } </routing-code> ? }
      <priority-id> { source.priorityId } </priority-id>
    </source>

  implicit def sourceXmlFormatV1 = Marshaller.opaque[Source, NodeSeq](marshalSourceXmlV1)
  
  /***************** XML NEW INSTANCE ********************/
  def marshalNewInstanceXmlV1(newInstance : NewInstance): NodeSeq =
    <new-instance>
      <id> { newInstance.actionId } </id>
      { <action-option-id> { newInstance.actionOptionId } </action-option-id> ? }
      <intervention> { newInstance.interventionId } </intervention>
      { <action-intervention-text> { newInstance.interventionText } </action-intervention-text> ? }
      <unit-id> { newInstance.unitId } </unit-id>
      { <routing-code> { newInstance.routingCode } </routing-code> ? }
      <priority-id> { newInstance.priorityId } </priority-id>
      <type-id> { newInstance.typeId } </type-id>
      { <type-option-id> { newInstance.typeOptionId } </type-option-id> ? }
    </new-instance>

  implicit def NewInstanceXmlFormatV1 = Marshaller.opaque[NewInstance, NodeSeq](marshalNewInstanceXmlV1)  
  /***************** XML ACTION ********************/
  //actionOn: ActionType, source: Option[Source], newInstance: Option[NewInstance]
  def marshalActionXmlV1(action: Action): NodeSeq =
    <action>
      <action-on> { action.actionOn } </action-on>
      { action.source.fold(NodeSeq.Empty)(source => marshalSourceXmlV1(source)) }
      { action.newInstance.fold(NodeSeq.Empty)(newInstance=> marshalNewInstanceXmlV1(newInstance)) }
    </action>

  implicit def actionXmlFormatV1 = Marshaller.opaque[Action, NodeSeq](marshalActionXmlV1)
  
  /***************** XML RULE ********************/
  
  def marshalRuleXmlV1(rule: Rule): NodeSeq =
    <rule>
      <id>{ rule.ruleId }</id>
      <pointId>{ rule.pointId }</pointId>
      <description>{ rule.description }</description>
      { rule.schemas.fold(NodeSeq.Empty)(l => marshalSchemasXmlV1(l)) }
      marshalConditionXmlV1(rule.condition)
			marshalActionXmlV1(rule.action)
      //rule.lastModification, rule.creationDate, rule.createdBy, 
    </rule>

  def marshalRulesXmlV1(rules: Iterable[Rule]) =
    <rules>
      { rules.map(marshalRuleXmlV1) }
    </rules>

  implicit def rulesXmlFormatV1 = Marshaller.opaque[Iterable[Rule], NodeSeq](marshalRulesXmlV1)

  implicit def ruleXmlFormatV1 = Marshaller.opaque[Rule, NodeSeq](marshalRuleXmlV1)

  /**
   * From the Iterable[Rule] value-object convert to a version and then marshal, wrap in an entity;
   * communicate with the VO in the API
   */
  implicit def personsMarshaller: ToResponseMarshaller[Iterable[Rule]] = Marshaller.oneOf(
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { rules =>
      HttpResponse(entity =
        HttpEntity(ContentType(MediaTypes.`application/json`), rules.map(rule => Rule(rule.ruleId, rule.pointId, rule.description, rule.schemas, rule.lastModification, rule.creationDate, rule.createdBy, rule.condition, rule.action)).toJson.compactPrint))
    },
    Marshaller.withFixedContentType(MediaVersionTypes.`application/groupsuite.routing.rule.v1+json`) { rules =>
      HttpResponse(entity =
        HttpEntity(ContentType(MediaVersionTypes.`application/groupsuite.routing.rule.v1+json`), rules.map(rule => Rule(rule.ruleId, rule.pointId, rule.description, rule.schemas, rule.lastModification, rule.creationDate, rule.createdBy, rule.condition, rule.action)).toJson.compactPrint))
    },
    Marshaller.withOpenCharset(MediaTypes.`application/xml`) { (rules, charset) =>
      HttpResponse(entity =
        HttpEntity.CloseDelimited(
          ContentType.WithCharset(MediaTypes.`application/xml`, HttpCharsets.`UTF-8`),
          akka.stream.scaladsl.Source.fromIterator(() => rules.iterator).mapAsync(1) { rule =>
            Marshal(rules.map(rule => Rule(rule.ruleId, rule.pointId, rule.description, rule.schemas, rule.lastModification, rule.creationDate, rule.createdBy, rule.condition, rule.action))).to[NodeSeq]
          }.map(ns => ByteString(ns.toString))))
    },
    Marshaller.withFixedContentType(MediaVersionTypes.`application/groupsuite.routing.rule.v1+xml`) { rules =>
      HttpResponse(entity =
        HttpEntity.CloseDelimited(
          ContentType(MediaVersionTypes.`application/groupsuite.routing.rule.v1+xml`),
          akka.stream.scaladsl.Source.fromIterator(() => rules.iterator).mapAsync(1) { rule =>
            Marshal(rules.map(rule => Rule(rule.ruleId, rule.pointId, rule.description, rule.schemas, rule.lastModification, rule.creationDate, rule.createdBy, rule.condition, rule.action))).to[NodeSeq]
          }.map(ns => ByteString(ns.toString))))
    })

  /**
   * From the Rule value-object convert to a version and then marshal, wrap in an entity;
   * communicate with the VO in the API
   */
  implicit def ruleMarshaller: ToResponseMarshaller[Rule] = Marshaller.oneOf(
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { rule =>
      HttpResponse(entity =
        HttpEntity(ContentType(MediaTypes.`application/json`), Rule(rule.ruleId, rule.pointId, rule.description, rule.schemas, rule.lastModification, rule.creationDate, rule.createdBy, rule.condition, rule.action).toJson.compactPrint))
    },
    Marshaller.withFixedContentType(MediaVersionTypes.`application/groupsuite.routing.rule.v1+json`) { rule =>
      HttpResponse(entity =
        HttpEntity(ContentType(MediaVersionTypes.`application/groupsuite.routing.rule.v1+json`), Rule(rule.ruleId, rule.pointId, rule.description, rule.schemas, rule.lastModification, rule.creationDate, rule.createdBy, rule.condition, rule.action).toJson.compactPrint))
    },
    Marshaller.withOpenCharset(MediaTypes.`application/xml`) { (rule, charset) =>
      HttpResponse(entity =
        HttpEntity.CloseDelimited(
          ContentType.WithCharset(MediaTypes.`application/xml`, HttpCharsets.`UTF-8`),
          akka.stream.scaladsl.Source.fromFuture(Marshal(Rule(rule.ruleId, rule.pointId, rule.description, rule.schemas, rule.lastModification, rule.creationDate, rule.createdBy, rule.condition, rule.action)).to[NodeSeq])
            .map(ns => ByteString(ns.toString))))
    },
    Marshaller.withFixedContentType(MediaVersionTypes.`application/groupsuite.routing.rule.v1+xml`) { rule =>
      HttpResponse(entity =
        HttpEntity.CloseDelimited(
          ContentType(MediaVersionTypes.`application/groupsuite.routing.rule.v1+xml`),
          akka.stream.scaladsl.Source.fromFuture(Marshal(Rule(rule.ruleId, rule.pointId, rule.description, rule.schemas, rule.lastModification, rule.creationDate, rule.createdBy, rule.condition, rule.action)).to[NodeSeq])
            .map(ns => ByteString(ns.toString))))
    })

//  // curl -X POST -H "Content-Type: application/xml" -d '<person><name>John Doe</name><age>25</age><married>true</married></person>' localhost:8080/person
//  def ruleXmlEntityUnmarshaller(implicit mat: Materializer): FromEntityUnmarshaller[Rule] =
//    Unmarshaller.byteStringUnmarshaller.forContentTypes(MediaTypes.`application/xml`).mapWithCharset { (data, charset) ⇒
//      val input: String = if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
//      val xml: Elem = XML.loadString(input)
//      val name: String = (xml \\ "name").text
//      val age: Int = (xml \\ "age").text.toInt
//      val married: Boolean = (xml \\ "married").text.toBoolean
//    }
//
//  // curl -X POST -H "Content-Type: application/vnd.acme.v1+xml" -d '<person><name>John Doe</name><age>25</age></person>' localhost:8080/person
//  def personXmlV1EntityUnmarshaller(implicit mat: Materializer): FromEntityUnmarshaller[Person] =
//    Unmarshaller.byteStringUnmarshaller.forContentTypes(MediaVersionTypes.`application/vnd.acme.v1+xml`).mapWithCharset { (data, charset) ⇒
//      val input: String = if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
//      val xml: Elem = XML.loadString(input)
//      val name: String = (xml \\ "name").text
//      val age: Int = (xml \\ "age").text.toInt
//      Person(name, age)
//    }

  // curl -X POST -H "Content-Type: application/json" -d '{"age": 25, "married": false, "name": "John Doe"}' localhost:8080/person
  def personJsonEntityUnmarshaller(implicit mat: Materializer): FromEntityUnmarshaller[Rule] =
    Unmarshaller.byteStringUnmarshaller.forContentTypes(MediaTypes.`application/json`).mapWithCharset { (data, charset) =>
      val input: String = if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
      val tmp = input.parseJson.convertTo[Rule]
      Rule(tmp.ruleId, tmp.pointId, tmp.description, tmp.schemas ,tmp.lastModification, tmp.creationDate, tmp.createdBy, tmp.condition, tmp.action)
    }

  // curl -X POST -H "Content-Type: application/vnd.acme.v1+json" -d '{"age": 25, "name": "John Doe"}' localhost:8080/person
  def personJsonV1EntityUnmarshaller(implicit mat: Materializer): FromEntityUnmarshaller[Rule] =
    Unmarshaller.byteStringUnmarshaller.forContentTypes(MediaVersionTypes.`application/groupsuite.routing.rule.v1+json`).mapWithCharset { (data, charset) =>
      val input: String = if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
      val tmp = input.parseJson.convertTo[Rule]
      Rule(tmp.ruleId, tmp.pointId, tmp.description, tmp.schemas ,tmp.lastModification, tmp.creationDate, tmp.createdBy, tmp.condition, tmp.action)
    }

  // will be used by the unmarshallers above
  implicit def personUnmarshaller(implicit mat: Materializer): FromEntityUnmarshaller[Rule] =
    Unmarshaller.firstOf[HttpEntity, Rule](
//      personXmlEntityUnmarshaller, personXmlV1EntityUnmarshaller,
      personJsonEntityUnmarshaller, personJsonV1EntityUnmarshaller)
}
