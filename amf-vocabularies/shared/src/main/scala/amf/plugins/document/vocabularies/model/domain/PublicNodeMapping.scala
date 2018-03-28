package amf.plugins.document.vocabularies.model.domain

import amf.client.model.StrField
import amf.core.metamodel.Obj
import amf.core.model.domain.DomainElement
import amf.core.parser.{Annotations, Fields}
import amf.plugins.document.vocabularies.metamodel.domain.DocumentMappingModel._
import amf.plugins.document.vocabularies.metamodel.domain.DocumentsModelModel._
import amf.plugins.document.vocabularies.metamodel.domain.PublicNodeMappingModel._
import amf.plugins.document.vocabularies.metamodel.domain.{
  DocumentMappingModel,
  DocumentsModelModel,
  PublicNodeMappingModel
}
import org.yaml.model.{YMap, YMapEntry, YNode}

case class PublicNodeMapping(fields: Fields, annotations: Annotations) extends DomainElement {

  def name(): StrField       = fields.field(Name)
  def mappedNode(): StrField = fields.field(MappedNode)

  def withName(name: String): PublicNodeMapping             = set(Name, name)
  def withMappedNode(mappedNode: String): PublicNodeMapping = set(MappedNode, mappedNode)

  override def adopted(parent: String): this.type = withId(parent)

  override def meta: Obj = PublicNodeMappingModel
}

object PublicNodeMapping {
  def apply(): PublicNodeMapping                         = apply(Annotations())
  def apply(ast: YMapEntry): PublicNodeMapping           = apply(Annotations(ast))
  def apply(annotations: Annotations): PublicNodeMapping = PublicNodeMapping(Fields(), annotations)
}

case class DocumentMapping(fields: Fields, annotations: Annotations) extends DomainElement {

  def documentName(): StrField                = fields.field(DocumentName)
  def encoded(): StrField                     = fields.field(EncodedNode)
  def declaredNodes(): Seq[PublicNodeMapping] = fields.field(DeclaredNodes)

  def withDocumentName(name: String): DocumentMapping   = set(DocumentName, name)
  def withEncoded(encodedNode: String): DocumentMapping = set(EncodedNode, encodedNode)
  def withDeclaredNodes(fragments: Seq[PublicNodeMapping]): DocumentMapping =
    setArrayWithoutId(DeclaredNodes, fragments)

  override def adopted(parent: String): this.type = withId(parent)

  override def meta: Obj = DocumentMappingModel
}

object DocumentMapping {
  def apply(): DocumentMapping                         = apply(Annotations())
  def apply(ast: YNode): DocumentMapping               = apply(Annotations(ast))
  def apply(annotations: Annotations): DocumentMapping = DocumentMapping(Fields(), annotations)
}

case class DocumentsModel(fields: Fields, annotations: Annotations) extends DomainElement {
  def root(): DocumentMapping           = fields.field(Root)
  def library(): DocumentMapping        = fields.field(Library)
  def fragments(): Seq[DocumentMapping] = fields.field(Fragments)

  def withRoot(documentMapping: DocumentMapping): DocumentsModel     = set(Root, documentMapping)
  def withLibrary(library: DocumentMapping): DocumentsModel          = set(Library, library)
  def withFragments(fragments: Seq[DocumentMapping]): DocumentsModel = setArrayWithoutId(Fragments, fragments)

  override def adopted(parent: String): this.type = withId(parent)

  override def meta: Obj = DocumentsModelModel
}

object DocumentsModel {
  def apply(): DocumentsModel                         = apply(Annotations())
  def apply(ast: YMap): DocumentsModel                = apply(Annotations(ast))
  def apply(annotations: Annotations): DocumentsModel = DocumentsModel(Fields(), annotations)
}