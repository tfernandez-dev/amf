package amf.plugins.features.validation

import java.io.{InputStreamReader, Reader, StringReader}
import java.nio.charset.Charset

import amf.AmfProfile
import amf.core.benchmark.ExecutionLog
import amf.core.model.document.BaseUnit
import amf.core.rdf.{RdfModel, RdfModelEmitter}
import amf.core.services.ValidationOptions
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.core.{ValidationReport, ValidationSpecification}
import amf.plugins.features.validation.emitters.ValidationRdfModelEmitter
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.jena.rdf.model.{Model, Resource}
import org.apache.jena.util.FileUtils
import org.topbraid.jenax.util.JenaUtil
import org.topbraid.shacl.js.{JSScriptEngine, JSScriptEngineFactory, NashornScriptEngine, SHACLScriptEngineManager}
import org.topbraid.shacl.validation.ValidationUtil

import scala.concurrent.{ExecutionContext, Future}

class SHACLValidator extends amf.core.validation.core.SHACLValidator with PlatformSecrets {

  var functionUrl: Option[String]  = None
  var functionCode: Option[String] = None

  val formats = Map(
    "application/ld+json" -> "JSON-LD",
    "application/json"    -> "JSON-LD",
    "JSON-LD"             -> "JSON-LD",
    "text/n3"             -> FileUtils.langN3,
    "test/turtle"         -> FileUtils.langTurtle
  )

  override def validate(data: String, dataMediaType: String, shapes: String, shapesMediaType: String)(
      implicit executionContext: ExecutionContext): Future[String] =
    Future {
      val dataModel   = loadModel(StringUtils.chomp(data), dataMediaType)
      val shapesModel = loadModel(StringUtils.chomp(shapes), shapesMediaType)
      loadLibrary()
      val res                      = SHACLScriptEngineManager.begin()
      var report: Option[Resource] = None
      try {
        SHACLScriptEngineManager.getCurrentEngine.executeScriptFromURL(NashornScriptEngine.RDFQUERY_JS)
        report = Some(SHACL.validate(dataModel, shapesModel))
      } finally {
        SHACLScriptEngineManager.end(res)
      }
      RDFPrinter(report.get.getModel, "JSON-LD")
    }

  private def loadModel(data: String, mediaType: String): Model = {
    formats.get(mediaType) match {
      case Some(format) =>
        val dataModel = JenaUtil.createMemoryModel()
        dataModel.read(IOUtils.toInputStream(data, Charset.defaultCharset()), "urn:dummy", format)
        dataModel
      case None => throw new Exception(s"Unsupported media type $mediaType")
    }
  }

  override def report(data: String, dataMediaType: String, shapes: String, shapesMediaType: String)(
      implicit executionContext: ExecutionContext): Future[ValidationReport] =
    validate(data, dataMediaType, shapes, shapesMediaType).map(new JVMValidationReport(_))

  /**
    * Registers a library in the validator
    */
  override def registerLibrary(url: String, code: String): Unit = {
    functionUrl = Some(url)
    functionCode = Some(code)
  }

  protected def loadLibrary(): Unit = {
    JSScriptEngineFactory.set(new JSScriptEngineFactory() {
      override def createScriptEngine: JSScriptEngine = new CachedScriptEngine(functionUrl, functionCode)
    })
  }

  override def validate(data: BaseUnit, shapes: Seq[ValidationSpecification], options: ValidationOptions)(
      implicit executionContext: ExecutionContext): Future[String] =
    Future {
      ExecutionLog.log("SHACLValidator#validate: loading Jena data model")
      val dataModel = new JenaRdfModel()
      new RdfModelEmitter(dataModel).emit(data, options.toRenderOptions)
      ExecutionLog.log("SHACLValidator#validate: loading Jena shapes model")
      val shapesModel = new JenaRdfModel()
      new ValidationRdfModelEmitter(options.messageStyle.profileName, shapesModel).emit(shapes)
      ExecutionLog.log("SHACLValidator#validate: loading library")
      loadLibrary()
      ExecutionLog.log("SHACLValidator#validate: starting script engine")
      val res                      = SHACLScriptEngineManager.begin()
      var report: Option[Resource] = None
      try {
        SHACLScriptEngineManager.getCurrentEngine.executeScriptFromURL(NashornScriptEngine.RDFQUERY_JS)
        ExecutionLog.log(
          s"SHACLValidator#validate: Number of data triples -> ${dataModel.model.listStatements().toList.size()}")
        ExecutionLog.log(
          s"SHACLValidator#validate: Number of shapes triples -> ${shapesModel.model.listStatements().toList.size()}")

        /*
    dataModel.dump()
    println("\n\n=======> SHAPES")
    println(shapesModel.toN3())
    println("\n\n=======> DATA")
    println(dataModel.toN3())
    val queryText =
      """
        |select (count(*) as ?total) ?name {
        | ?s a <http://www.w3.org/ns/shacl#NodeShape> .
        | ?s <http://www.w3.org/ns/shacl#name> ?name
        |}
        |group by ?name
        |order by desc(?total)
      """.stripMargin

    val query = QueryFactory.create(queryText)
    val queryExec = QueryExecutionFactory.create(query, dataModel.model)
    val results = queryExec.execSelect()
    while (results.hasNext) {
      val solution = results.next()
      val varNames = solution.varNames()
      while (varNames.hasNext) {
        val varName = varNames.next()
        println(s"$varName -> ${solution.get(varName)}")
      }
    }
         */
        ExecutionLog.log(s"SHACLValidator#validate: validating...")
        report = Some(SHACL.validate(dataModel.model, shapesModel.model))
      } finally {
        ExecutionLog.log(s"SHACLValidator#validate: releasing script manager resources")
        SHACLScriptEngineManager.end(res)
      }
      ExecutionLog.log(s"SHACLValidator#validate: Generating JSON-LD report")
      val output = RDFPrinter(report.get.getModel, "JSON-LD")
      ExecutionLog.log(s"SHACLValidator#validate: finishing")
      output
    }

  override def report(data: BaseUnit, shapes: Seq[ValidationSpecification], options: ValidationOptions)(
      implicit executionContext: ExecutionContext): Future[ValidationReport] =
    validate(data, shapes, options: ValidationOptions).map(new JVMValidationReport(_))

  override def shapes(shapes: Seq[ValidationSpecification], functionsUrl: String): RdfModel = {
    val shapesModel = new JenaRdfModel()
    new ValidationRdfModelEmitter(AmfProfile, shapesModel).emit(shapes)
    shapesModel
  }

  override def emptyRdfModel(): RdfModel = new JenaRdfModel()
}

class CachedScriptEngine(functionUrl: Option[String], functionCode: Option[String])
    extends org.topbraid.shacl.js.NashornScriptEngine() {
  @throws[Exception]
  override protected def createScriptReader(url: String): Reader = {
    if (NashornScriptEngine.DASH_JS.equals(url)) {
      new InputStreamReader(classOf[NashornScriptEngine].getResourceAsStream("/js/dash.js"))
    } else if (NashornScriptEngine.RDFQUERY_JS.equals(url)) {
      new InputStreamReader(classOf[NashornScriptEngine].getResourceAsStream("/js/rdfquery.js"))
    } else if (functionUrl.isDefined && functionUrl.get.equals(functionUrl.get)) {
      new StringReader(functionCode.get)
    } else {
      new InputStreamReader(new java.net.URL(url).openStream)
    }
  }
}

private object SHACL {
  def validate(dataModel: Model, shapesModel: Model): Resource = synchronized {
    ValidationUtil.validateModel(dataModel, shapesModel, false)
  }
}
