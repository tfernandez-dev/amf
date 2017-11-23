package amf.metadata.domain.extensions

import amf.domain.extensions.DomainExtension
import amf.framework.metamodel.Field
import amf.framework.metamodel.Type.Str
import amf.metadata.domain.{DomainElementModel, KeyField}
import amf.vocabulary.Namespace.{Document, Http}
import amf.vocabulary.ValueType

/**
  * Extension to the model being parsed from RAML annotation or OpenAPI extensions.
  * They must be a DomainPropertySchema (only in RAML) defining them.
  * The DomainPropertySchema might have an associated Data Shape that must validate
  * the extension nested graph.
  *
  * They are parsed as RDF graphs using a default transformation from a set of nested
  * records into RDF
  */
object DomainExtensionModel extends DomainElementModel with KeyField {

  val Name      = Field(Str, Document + "name")
  val DefinedBy = Field(CustomDomainPropertyModel, Document + "definedBy")
  val Extension = Field(DataNodeModel, Document + "extension")

  override val key: Field = Name

  override def fields: List[Field] = List(Name, DefinedBy, Extension) ++ DomainElementModel.fields

  override val `type`: List[ValueType] = Http + "DomainExtension" :: DomainElementModel.`type`

  override def modelInstance = DomainExtension()
}
