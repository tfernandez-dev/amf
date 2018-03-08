package amf.plugins.domain.webapi.metamodel

import amf.core.metamodel.Field
import amf.core.metamodel.Type._
import amf.core.metamodel.domain.DomainElementModel
import amf.core.metamodel.domain.templates.KeyField
import amf.plugins.domain.webapi.metamodel.security.ParametrizedSecuritySchemeModel
import amf.plugins.domain.webapi.models.EndPoint
import amf.core.vocabulary.Namespace.{Http, Hydra, Schema}
import amf.core.vocabulary.{Namespace, ValueType}

/**
  * EndPoint metamodel
  *
  * EndPoint in the API holding a number of executable operations
  */
object EndPointModel extends DomainElementModel with KeyField {

  val Path = Field(Str, Http + "path")

  val Name = Field(Str, Schema + "name")

  val Description = Field(Str, Schema + "description")

  val Operations = Field(Array(OperationModel), Hydra + "supportedOperation")

  val Parameters = Field(Array(ParameterModel), Http + "parameter")

  val Payloads = Field(Array(PayloadModel), Http + "payload")

  val Security = Field(Array(ParametrizedSecuritySchemeModel), Namespace.Security + "security")

  override val key: Field = Path

  override val `type`: List[ValueType] = Http + "EndPoint" :: DomainElementModel.`type`

  override def fields: List[Field] =
    List(Path, Name, Description, Operations, Parameters, Payloads, Security) ++ DomainElementModel.fields

  override def modelInstance = EndPoint()
}
