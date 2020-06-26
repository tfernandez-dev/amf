package amf.cyclic

import amf.core.CompilerContextBuilder
import amf.core.model.document.BaseUnit
import amf.core.parser.UnspecifiedReference
import amf.core.parser.errorhandler.{ParserErrorHandler, UnhandledParserErrorHandler}
import amf.core.remote.{Amf, Oas20, OasJsonHint}
import amf.core.resolution.pipelines.ResolutionPipeline
import amf.core.services.RuntimeCompiler
import amf.facades.Validation
import amf.io.FunSuiteCycleTests
import amf.resolution.ResolutionTest

import scala.concurrent.Future

class OasCyclicFiles extends ResolutionTest {
  override val defaultPipelineToUse: String = ResolutionPipeline.CACHE_PIPELINE

  override def basePath: String = "amf-client/shared/src/test/resources/cyclic/oas/"

  test("Oas non-recursive cyclic files") {
    cycle("non-recursive/api.json", "non-recursive/api.jsonld", OasJsonHint, Amf, transformWith = Some(Oas20))
  }

  test("Oas recursive cyclic files") {
    cycle("recursive/api.json",
          "recursive/api.jsonld",
          OasJsonHint,
          Amf,
          transformWith = Some(Oas20),
          eh = Some(UnhandledParserErrorHandler))
  }

  test("Oas recursive cyclic files with inline fragment") {
    cycle("inline-recursive/api.json", "inline-recursive/api.jsonld", OasJsonHint, Amf, transformWith = Some(Oas20))
  }
}

class OasParsedCyclicFiles extends FunSuiteCycleTests {

  override def basePath: String = "amf-client/shared/src/test/resources/cyclic/oas/"

  test("Oas recursive cyclic files") {
    cycle("parsed-recursive/api.json", "parsed-recursive/api.jsonld", OasJsonHint, Amf)
  }

  test("Oas recursive cyclic files with inline fragment") {
    cycle("parsed-inline-recursive/api.json", "parsed-inline-recursive/api.jsonld", OasJsonHint, Amf)
  }
}
