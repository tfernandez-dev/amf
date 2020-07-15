package amf.emit

import amf.client.parse.DefaultParserErrorHandler
import amf.core.client.ParsingOptions
import amf.core.model.document.{BaseUnit, DeclaresModel, EncodesModel}
import amf.core.model.domain.DomainElement
import amf.core.parser.SyamlParsedDocument
import amf.core.parser.errorhandler.{ParserErrorHandler, UnhandledParserErrorHandler}
import amf.core.remote.{Hint, RamlYamlHint, Vendor}
import amf.facades.{AMFCompiler, Validation}
import amf.io.FileAssertionTest
import amf.plugins.document.webapi.parser.spec.common.emitters.DomainElementEmitter
import amf.plugins.domain.webapi.models.{Response, WebApi}
import amf.plugins.syntax.SYamlSyntaxPlugin
import org.scalatest.{Assertion, AsyncFunSuite}
import org.yaml.model.{YDocument, YNode}

import scala.concurrent.Future

class Raml10DomainElementEmission extends DomainElementEmitterTest {

  val basePath: String = "amf-client/shared/src/test/resources/org/raml/cycle/"
  val vendor: Vendor   = Vendor.RAML10

  test("initial test") {
    renderElement(
      "response/input.raml",
      CommonExtractors.FIRST_RESPONSE,
      "response/output.yaml",
      RamlYamlHint
    )
  }

}

object CommonExtractors {

  val FIRST_RESPONSE: BaseUnit => Option[Response] = {
    case e: EncodesModel =>
      Some(e.encodes.asInstanceOf[WebApi].endPoints.head.operations.head.responses.head)
    case _ => None
  }

}
trait DomainElementEmitterTest extends AsyncFunSuite with FileAssertionTest {

  case class EmissionConfig(source: String, golden: String, hint: Hint, directory: String) {
    def goldenPath: String = directory + golden
    def sourcePath: String = directory + source
  }
  def basePath: String
  def vendor: Vendor

  def renderElement(source: String,
                    extractor: BaseUnit => Option[DomainElement],
                    golden: String,
                    hint: Hint,
                    directory: String = basePath): Future[Assertion] = {

    val config = EmissionConfig(source, golden, hint, directory)
    build(config, Some(DefaultParserErrorHandler.withRun()))
      .map(extractor)
      .flatMap(render)
      .flatMap(writeTemporaryFile(golden))
      .flatMap(assertDifferences(_, config.goldenPath))
  }

  private def build(config: EmissionConfig, eh: Option[ParserErrorHandler]): Future[BaseUnit] = {
    Validation(platform).flatMap { _ =>
      var options = ParsingOptions().withoutAmfJsonLdSerialization
      options = options.withBaseUnitUrl(s"file://${config.goldenPath}")
      AMFCompiler(s"file://${config.sourcePath}",
                  platform,
                  config.hint,
                  eh = eh.getOrElse(UnhandledParserErrorHandler),
                  parsingOptions = options).build()
    }
  }

  private def render(element: Option[DomainElement]): Future[String] = {
    Future { renderDomainElement(element) }
  }

  private def renderDomainElement(shape: Option[DomainElement]): String = {
    val node     = shape.map(DomainElementEmitter.emit(_, vendor)).getOrElse(YNode.Empty)
    val document = SyamlParsedDocument(document = YDocument(node))
    SYamlSyntaxPlugin.unparse("application/yaml", document).getOrElse("").toString
  }

}
