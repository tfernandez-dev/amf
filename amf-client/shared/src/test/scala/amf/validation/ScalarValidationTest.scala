package amf.validation

import amf.core.AMF
import amf.core.model.DataType._
import amf.core.parser.{Annotations, Fields}
import amf.plugins.document.webapi.validation.PayloadValidatorPlugin
import amf.plugins.domain.shapes.models.{AnyShape, ArrayShape, NodeShape, ScalarShape}
import org.scalatest.{AsyncFunSuite, Matchers}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class ScalarValidationTest extends AsyncFunSuite with Matchers {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val APPLICATION_JSON = "application/json"
  val APPLICATION_YAML = "application/yaml"

  val Node  = "NodeObject"
  val Array = "ArrayObject"

  val expectedConforms = List(
    ("1", List(String, Integer, Number)),
    ("\"1\"", List(String)),
    ("some", List(String)),
    ("\"some\"", List(String)),
    ("true", List(String, Boolean)),
    ("\"true\"", List(String)),
    ("null", List(Nil)),
    ("\"null\"", List(String)),
    ("{some: true}", List(String)),
    ("{\"some\": true}", List(String, Node)),
    ("[\"some\", true]", List(String, Array)),
    ("[some, true]", List(String)),
    ("2019-01-01", List(String, Date)),
    ("2015-07-04T21:00:00", List(String, DateTimeOnly)),
    ("2016-02-28T16:41:41.090Z", List(String, DateTime)),
    ("\"2016-02-28T16:41:41.090Z\"", List(String, DateTime)),
    ("-5.5", List(String, Number)),
    ("\"-5.5\"", List(String)),
    ("1.07E7", List(String, Number))
  )

  val shapes = Map(
    String       -> new ScalarShape(Fields(), Annotations()).withDataType(String),
    Integer      -> new ScalarShape(Fields(), Annotations()).withDataType(Integer),
    Boolean      -> new ScalarShape(Fields(), Annotations()).withDataType(Boolean),
    Nil          -> new ScalarShape(Fields(), Annotations()).withDataType(Nil),
    Date         -> new ScalarShape(Fields(), Annotations()).withDataType(Date),
    DateTime     -> new ScalarShape(Fields(), Annotations()).withDataType(DateTime),
    DateTimeOnly -> new ScalarShape(Fields(), Annotations()).withDataType(DateTimeOnly),
    Number       -> new ScalarShape(Fields(), Annotations()).withDataType(Number),
    Node         -> new NodeShape(Fields(), Annotations()),
    Array        -> new ArrayShape(Fields(), Annotations())
  )

  makeFixtureRuns.map {
    case Fixture(scalarType, toValidate, conforms) => {
      test(
        s"Validate ${surroundWithSimpleQuotes(toValidate.toString)} as ${formatDataType(scalarType)} toBe ${conforms}") {
        initAmf.flatMap { _ =>
          val shape                   = shapes(scalarType)
          val jsonPayloadValidation   = validateWithJsonPayload(scalarType, toValidate, shape)
          val yamlParameterValidation = validateWithYamlParameter(toValidate, shape)
          val validations             = List(jsonPayloadValidation, yamlParameterValidation)
          Future
            .sequence(validations)
            .map(x => {
              Result(jsonValidation = x.head, yamlValidation = x.last) shouldEqual Result(jsonValidation = conforms,
                                                                                          yamlValidation = conforms)
            })
        }
      }
    }
  }

  private def validateWithYamlParameter(toValidate: Any, shape: AnyShape) = {
    val validator = shape.parameterValidator(APPLICATION_YAML).get
    validator
      .validate(APPLICATION_YAML, toValidate.toString)
      .map(report => report.conforms)
  }

  private def validateWithJsonPayload(scalarType: String, toValidate: Any, shape: AnyShape) = {
    val validator = shape.payloadValidator(APPLICATION_JSON).get
    val valueForJson = converters
      .get(scalarType)
      .map(c => c.format(toValidate.toString))
      .getOrElse(toValidate.toString)
    validator
      .validate(APPLICATION_JSON, valueForJson)
      .map(report => report.conforms)
  }

  private def formatDataType(dataType: String) = dataType.split("#").last

  private def initAmf: Future[Unit] =
    AMF
      .init()
      .map(_ => {
        AMF.registerPlugin(PayloadValidatorPlugin)
      })

  case class Result(jsonValidation: Boolean, yamlValidation: Boolean)

  case class Fixture(scalarType: String, toValidate: Any, result: Boolean)

  private def makeFixtureRuns: List[Fixture] = {
    expectedConforms.iterator.toList.flatMap({
      case (example, typesThatShouldConform) =>
        val availableTypes = shapes.keySet
        availableTypes.map(t => Fixture(t, example, typesThatShouldConform.contains(t)))
    })
  }

  private def surroundWithSimpleQuotes(s: String) = "\'" + s + "\'"

  val converters = Map(
    String       -> new StringFormatter,
    Date         -> new StringFormatter,
    DateTimeOnly -> new StringFormatter,
    DateTime     -> new StringFormatter
  )

  sealed trait Formatter {
    def format(format: String): String
  }

  class StringFormatter extends Formatter {
    override def format(format: String): String =
      if (format.equals("null")) format
      else if (!startsAndEndsWith("\"", format) && !startsAndEndsWith("\'", format)) quote(format)
      else if (startsAndEndsWith("\"", format) && format.length == 1) quote(format)
      else format

    private def startsAndEndsWith(symbol: String, toCheck: String) =
      toCheck.startsWith(symbol) && toCheck.endsWith(symbol)

    private def quote(something: String): String = "\"" + something + "\""
  }
}
