package amf.plugins.document.webapi

import amf.ProfileName
import amf.client.plugins.{AMFDocumentPlugin, AMFPlugin, AMFValidationPlugin}
import amf.core.annotations.{DeclaredElement, ExternalFragmentRef, InlineElement}
import amf.core.emitter.RenderOptions
import amf.core.metamodel.document.FragmentModel
import amf.core.model.document.BaseUnit
import amf.core.model.domain.AnnotationGraphLoader
import amf.core.parser.ErrorHandler
import amf.core.remote.{Platform, Vendor}
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.core.ValidationProfile
import amf.core.validation.{AMFValidationReport, EffectiveValidations}
import amf.internal.environment.Environment
import amf.plugins.document.webapi.annotations._
import amf.plugins.document.webapi.contexts.SpecEmitterContext
import amf.plugins.document.webapi.metamodel.FragmentsTypesModels._
import amf.plugins.document.webapi.metamodel.{ExtensionModel, OverlayModel}
import amf.plugins.document.webapi.references.WebApiReferenceHandler
import amf.plugins.document.webapi.validation.WebApiValidations
import amf.plugins.domain.shapes.DataShapesDomainPlugin
import amf.plugins.domain.webapi.WebAPIDomainPlugin
import amf.plugins.features.validation.ParserSideValidations

import scala.concurrent.Future

trait BaseWebApiPlugin extends AMFDocumentPlugin with AMFValidationPlugin with WebApiValidations with PlatformSecrets {

  protected def vendor: Vendor

  override val ID: String = vendor.name

  override def referenceHandler(eh: ErrorHandler) = new WebApiReferenceHandler(ID, this)

  override def dependencies(): Seq[AMFPlugin] = Seq(WebAPIDomainPlugin, DataShapesDomainPlugin, ExternalJsonRefsPlugin)

  def specContext(options: RenderOptions): SpecEmitterContext

  /**
    * Does references in this type of documents be recursive?
    */
  override val allowRecursiveReferences: Boolean = false

  override def modelEntities: Seq[FragmentModel] = Seq(
    ExtensionModel,
    OverlayModel,
    DocumentationItemFragmentModel,
    DataTypeFragmentModel,
    NamedExampleFragmentModel,
    ResourceTypeFragmentModel,
    TraitFragmentModel,
    AnnotationTypeDeclarationFragmentModel,
    SecuritySchemeFragmentModel
  )

  override def serializableAnnotations(): Map[String, AnnotationGraphLoader] = Map(
    "parsed-json-schema"         -> ParsedJSONSchema,
    "external-fragment-ref"      -> ExternalFragmentRef,
    "json-schema-id"             -> JSONSchemaId,
    "declared-element"           -> DeclaredElement,
    "inline-element"             -> InlineElement,
    "local-link-path"            -> LocalLinkPath,
    "extension-provenance"       -> ExtensionProvenance,
    "form-body-parameter"        -> FormBodyParameter,
    "parameter-name-for-payload" -> ParameterNameForPayload,
    "required-param-payload"     -> RequiredParamPayload
  )

  override def resolve(unit: BaseUnit, errorHandler: ErrorHandler, pipelineId: String): BaseUnit = {
    errorHandler.violation(
      ParserSideValidations.ResolutionErrorSpecification.id,
      s"Unsupported '$pipelineId' in Raml10Plugin",
      unit.location().getOrElse(unit.id)
    )
    unit
  }

  val validationProfile: ProfileName

  /**
    * Validation profiles supported by this plugin by default
    */
  // todo: compute again each map for each web api vendor plug in (ej raml 10 oas 20 etc). Filter each one by vendor? compute only one time the map? the problem its how to add custom validations.
  override def domainValidationProfiles(platform: Platform): Map[String, () => ValidationProfile] =
    defaultValidationProfiles

  def validationRequest(baseUnit: BaseUnit,
                        profile: ProfileName,
                        validations: EffectiveValidations,
                        platform: Platform,
                        env: Environment): Future[AMFValidationReport] = {
    validationRequestsForBaseUnit(baseUnit, profile, validations, validationProfile.messageStyle, platform, env)
  }

  override def init(): Future[AMFPlugin] = Future.successful(this)

}
