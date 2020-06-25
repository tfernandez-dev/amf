package amf.cyclic

import amf.core.{AMF, CompilerContextBuilder}
import amf.core.model.document.BaseUnit
import amf.core.parser.UnspecifiedReference
import amf.core.parser.errorhandler.{ParserErrorHandler, UnhandledParserErrorHandler}
import amf.core.remote.{Amf, Oas20, OasJsonHint}
import amf.core.resolution.pipelines.ResolutionPipeline
import amf.core.services.RuntimeCompiler
import amf.facades.Validation
import amf.internal.environment.Environment
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

class OasRuntimeCompilerParsedCyclicFiles extends ResolutionTest {

  override def basePath: String = "amf-client/shared/src/test/resources/cyclic/oas/"

  test("Oas recursive cyclic files with fragment as root") {
    cycle("parsed-recursive/ref.json",
          "parsed-recursive/ref.jsonld",
          OasJsonHint,
          Amf,
          eh = Some(UnhandledParserErrorHandler))
  }

  // force AMF to enable a json external fragment as root to test it doesn't throw CyclicReferenceException
  override def build(config: CycleConfig,
                     eh: Option[ParserErrorHandler],
                     useAmfJsonldSerialisation: Boolean): Future[BaseUnit] = {
    AMF.init().flatMap { _ =>
      val path                   = config.sourcePath
      val finalPath              = if (path.startsWith("file://")) path else s"file://$path"
      val compilerContextBuilder = new CompilerContextBuilder(s"$finalPath", platform, UnhandledParserErrorHandler)
      RuntimeCompiler
        .forContext(
          compilerContextBuilder.withEnvironment(Environment()).build(),
          None,
          None,
          UnspecifiedReference
        )
    }
  }
}
