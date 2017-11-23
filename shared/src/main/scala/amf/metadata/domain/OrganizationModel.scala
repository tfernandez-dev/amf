package amf.metadata.domain

import amf.framework.metamodel.Field
import amf.framework.metamodel.Type.{Iri, Str}
import amf.vocabulary.Namespace.Schema
import amf.vocabulary.ValueType

/**
  * Organization metamodel
  */
object OrganizationModel extends DomainElementModel {

  val Url = Field(Iri, Schema + "url")

  val Name = Field(Str, Schema + "name")

  val Email = Field(Str, Schema + "email")

  override val `type`: List[ValueType] = Schema + "Organization" :: DomainElementModel.`type`

  override def fields: List[Field] = List(Url, Name, Email) ++ DomainElementModel.fields
}
