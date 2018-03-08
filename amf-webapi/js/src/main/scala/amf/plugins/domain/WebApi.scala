package amf.plugins.domain

import amf.core.unsafe.PlatformSecrets
import amf.client.model.domain._
import amf.plugins.domain.webapi.metamodel.templates
import amf.plugins.domain.webapi.{WebAPIDomainPlugin, metamodel, models}

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
object WebApi extends PlatformSecrets {
  def register() = {
    platform.registerWrapper(metamodel.EndPointModel) {
      case s: models.EndPoint => EndPoint(s)
    }
    platform.registerWrapper(metamodel.LicenseModel) {
      case s: models.License => License(s)
    }
    platform.registerWrapper(metamodel.OperationModel) {
      case s: models.Operation => Operation(s)
    }
    platform.registerWrapper(metamodel.OrganizationModel) {
      case s: models.Organization => Organization(s)
    }
    platform.registerWrapper(metamodel.ParameterModel) {
      case s: models.Parameter => Parameter(s)
    }
    platform.registerWrapper(templates.ParametrizedResourceTypeModel) {
      case s: models.templates.ParametrizedResourceType => ParametrizedResourceType(s)
    }
    platform.registerWrapper(templates.ParametrizedTraitModel) {
      case s: models.templates.ParametrizedTrait => ParametrizedTrait(s)
    }
    platform.registerWrapper(metamodel.security.ParametrizedSecuritySchemeModel) {
      case s: models.security.ParametrizedSecurityScheme => ParametrizedSecurityScheme(s)
    }
    platform.registerWrapper(metamodel.security.SecuritySchemeModel) {
      case s: models.security.SecurityScheme => SecurityScheme(s)
    }
    platform.registerWrapper(metamodel.PayloadModel) {
      case s: models.Payload => Payload(s)
    }
    platform.registerWrapper(metamodel.RequestModel) {
      case s: models.Request => Request(s)
    }
    platform.registerWrapper(metamodel.ResponseModel) {
      case s: models.Response => Response(s)
    }
    platform.registerWrapper(metamodel.security.ScopeModel) {
      case s: models.security.Scope => Scope(s)
    }
    platform.registerWrapper(metamodel.security.SettingsModel) {
      case s: models.security.Settings => new Settings(s)
    }
    platform.registerWrapper(metamodel.WebApiModel) {
      case s: models.WebApi => new WebApi(s)
    }
    platform.registerWrapper(metamodel.templates.TraitModel) {
      case s: models.templates.Trait => Trait(s)
    }
    platform.registerWrapper(metamodel.templates.ResourceTypeModel) {
      case s: models.templates.ResourceType => ResourceType(s)
    }

    amf.Core.registerPlugin(WebAPIDomainPlugin)
  }
}
