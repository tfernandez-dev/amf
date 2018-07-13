package amf.plugins.domain.webapi.metamodel.security

import amf.core.metamodel.Field
import amf.core.metamodel.Type.{Array, Str}
import amf.core.metamodel.domain.{DataNodeModel, DomainElementModel}
import amf.plugins.domain.webapi.models.security.{ApiKeySettings, OAuth1Settings, OAuth2Settings, Settings}
import amf.core.vocabulary.Namespace.Security
import amf.core.vocabulary.ValueType

trait SettingsModel extends DomainElementModel

object SettingsModel extends SettingsModel {
  val AdditionalProperties = Field(DataNodeModel, Security + "additionalProperties")

  override val `type`: List[ValueType] = List(Security + "Settings") ++ DomainElementModel.`type`

  override def fields: List[Field] = List(AdditionalProperties) ++ DomainElementModel.fields

  override def modelInstance = Settings()
}

object OAuth1SettingsModel extends SettingsModel {

  val RequestTokenUri = Field(Str, Security + "requestTokenUri")

  val AuthorizationUri = Field(Str, Security + "authorizationUri")

  val TokenCredentialsUri = Field(Str, Security + "tokenCredentialsUri")

  val Signatures = Field(Array(Str), Security + "signature")

  override val `type`: List[ValueType] = List(Security + "OAuth1Settings") ++ SettingsModel.`type`

  override def fields: List[Field] =
    List(RequestTokenUri, AuthorizationUri, TokenCredentialsUri, Signatures) ++ SettingsModel.fields

  override def modelInstance = OAuth1Settings()
}

object OAuth2SettingsModel extends SettingsModel {

  val AuthorizationUri = Field(Str, Security + "authorizationUri")

  val AccessTokenUri = Field(Str, Security + "accessTokenUri")

  val AuthorizationGrants = Field(Array(Str), Security + "authorizationGrant")

  val Flow = Field(Str, Security + "flow")

  val RefreshUri = Field(Str, Security + "refreshUri")

  val Scopes = Field(Array(ScopeModel), Security + "scope")

  override val `type`: List[ValueType] = List(Security + "OAuth2Settings") ++ SettingsModel.`type`

  override def fields: List[Field] =
    List(AuthorizationUri, AccessTokenUri, AuthorizationGrants, Flow, RefreshUri, Scopes) ++ SettingsModel.fields

  override def modelInstance = OAuth2Settings()
}

object ApiKeySettingsModel extends SettingsModel {

  val Name = Field(Str, Security + "name")

  val In = Field(Str, Security + "in")

  override val `type`: List[ValueType] = List(Security + "ApiKeySettings") ++ SettingsModel.`type`

  override def fields: List[Field] = List(Name, In) ++ SettingsModel.fields

  override def modelInstance = ApiKeySettings()
}

object HttpSettingsModel extends SettingsModel {

  val Scheme = Field(Str, Security + "scheme")

  val BearerFormat = Field(Str, Security + "bearerFormat")

  override val `type`: List[ValueType] = List(Security + "HttpSettings") ++ SettingsModel.`type`

  override def fields: List[Field] = List(Scheme, BearerFormat) ++ SettingsModel.fields

  override def modelInstance = ApiKeySettings()
}

object OpenIdConnectSettingsModel extends SettingsModel {

  val Url = Field(Str, Security + "openIdConnectUrl")

  override val `type`: List[ValueType] = List(Security + "OpenIdConnectSettings") ++ SettingsModel.`type`

  override def fields: List[Field] = List(Url) ++ SettingsModel.fields

  override def modelInstance = ApiKeySettings()
}