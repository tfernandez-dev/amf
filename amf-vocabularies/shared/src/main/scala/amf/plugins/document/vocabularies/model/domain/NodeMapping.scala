package amf.plugins.document.vocabularies.model.domain

import amf.core.metamodel.Obj
import amf.core.model.StrField
import amf.core.model.domain.{DomainElement, Linkable}
import amf.core.parser.{Annotations, Fields}
import amf.core.utils._
import amf.plugins.document.vocabularies.metamodel.domain.NodeMappingModel
import amf.plugins.document.vocabularies.metamodel.domain.NodeMappingModel._
import org.yaml.model.YMap

case class NodeMapping(fields: Fields, annotations: Annotations) extends DomainElement with Linkable {

  override def meta: Obj = NodeMappingModel

  def name: StrField                            = fields.field(Name)
  def nodetypeMapping: StrField                 = fields.field(NodeTypeMapping)
  def propertiesMapping(): Seq[PropertyMapping] = fields.field(PropertiesMapping)
    def idTemplate: StrField                      = fields.field(IdTemplate)

  def withName(name: String): NodeMapping                             = set(Name, name)
  def withNodeTypeMapping(nodeType: String): NodeMapping              = set(NodeTypeMapping, nodeType)
  def withPropertiesMapping(props: Seq[PropertyMapping]): NodeMapping = setArrayWithoutId(PropertiesMapping, props)
  def withIdTemplate(idTemplate: String): NodeMapping                 = set(IdTemplate, idTemplate)

  override def linkCopy(): Linkable = NodeMapping().withId(id)

  override def resolveUnreferencedLink[T](label: String, annotations: Annotations, unresolved: T): T = {
    val unresolvedNodeMapping = unresolved.asInstanceOf[NodeMapping]
    unresolvedNodeMapping
      .link(label, annotations)
      .asInstanceOf[NodeMapping]
      .withId(unresolvedNodeMapping.id)
      .withName(unresolvedNodeMapping.name.value())
      .asInstanceOf[T]
  }

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = {
    "/" + name.value().urlEncoded
  }
}

object NodeMapping {
  def apply(): NodeMapping = apply(Annotations())

  def apply(ast: YMap): NodeMapping = apply(Annotations(ast))

  def apply(annotations: Annotations): NodeMapping = NodeMapping(Fields(), annotations)
}
