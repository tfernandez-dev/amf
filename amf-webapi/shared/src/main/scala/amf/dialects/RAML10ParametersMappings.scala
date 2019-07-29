package amf.dialects

import amf.core.vocabulary.Namespace.XsdTypes.xsdBoolean
import amf.dialects.RAML10Dialect.DialectLocation
import amf.plugins.document.vocabularies.model.domain.{NodeMapping, PropertyMapping}
import amf.plugins.domain.webapi.metamodel.ParameterModel
object RAML10ParametersMappings {

  private val requiredProperty: PropertyMapping = PropertyMapping()
    .withId(DialectLocation + "#/declarations/ParameterNode/required")
    .withNodePropertyMapping(ParameterModel.Required.value.iri())
    .withName("required")
    .withLiteralRange(xsdBoolean.iri())

  val ParameterAnyShapeNode: NodeMapping = NodeMapping()
    .withId(DialectLocation + "#/declarations/ParameterAnyShapeNode")
    .withName("ParameterAnyShapeNode")
    .withNodeTypeMapping(ParameterModel.`type`.head.iri())
    .withPropertiesMapping(RAML10DataTypesMappings.anyShapeProperties :+ requiredProperty)

  val ParameterNodeShapeNode: NodeMapping = NodeMapping()
    .withId(DialectLocation + "#/declarations/ParameterNodeShapeNode")
    .withName("ParameterNodeShapeNode")
    .withNodeTypeMapping(ParameterModel.`type`.head.iri())
    .withPropertiesMapping(RAML10DataTypesMappings.NodeShapeNode.propertiesMapping() :+ requiredProperty)

  val ParameterArrayShapeNode: NodeMapping = NodeMapping()
    .withId(DialectLocation + "#/declarations/ParameterArrayShapeNode")
    .withName("ParameterArrayShapeNode")
    .withNodeTypeMapping(ParameterModel.`type`.head.iri())
    .withPropertiesMapping(RAML10DataTypesMappings.ArrayShapeNode.propertiesMapping() :+ requiredProperty)

  val ParameterUnionShapeNode: NodeMapping = NodeMapping()
    .withId(DialectLocation + "#/declarations/ParameterUnionShapeNode")
    .withName("ParameterUnionShapeNode")
    .withNodeTypeMapping(ParameterModel.`type`.head.iri())
    .withPropertiesMapping(RAML10DataTypesMappings.UnionShapeNode.propertiesMapping() :+ requiredProperty)

  val ParameterStringShapeNode: NodeMapping = NodeMapping()
    .withId(DialectLocation + "#/declarations/ParameterStringShapeNode")
    .withName("ParameterStringShapeNode")
    .withNodeTypeMapping(ParameterModel.`type`.head.iri())
    .withPropertiesMapping(RAML10DataTypesMappings.StringShapeNode.propertiesMapping() :+ requiredProperty)

  val ParameterNumberShapeNode: NodeMapping = NodeMapping()
    .withId(DialectLocation + "#/declarations/ParameterNumberShapeNode")
    .withName("ParameterNumberShapeNode")
    .withNodeTypeMapping(ParameterModel.`type`.head.iri())
    .withPropertiesMapping(RAML10DataTypesMappings.NumberShapeNode.propertiesMapping() :+ requiredProperty)

  val ParameterFileShapeNode: NodeMapping = NodeMapping()
    .withId(DialectLocation + "#/declarations/ParameterFileShapeNode")
    .withName("ParameterFileShapeNode")
    .withNodeTypeMapping(ParameterModel.`type`.head.iri())
    .withPropertiesMapping(RAML10DataTypesMappings.FileShapeNode.propertiesMapping() :+ requiredProperty)

  val ParameterNilShapeNode: NodeMapping = NodeMapping()
    .withId(DialectLocation + "#/declarations/ParameterNilShapeNode")
    .withName("ParameterNilShapeNode")
    .withNodeTypeMapping(ParameterModel.`type`.head.iri())
    .withPropertiesMapping(RAML10DataTypesMappings.NilShapeNode.propertiesMapping() :+ requiredProperty)

  val parametersDeclarations: Seq[NodeMapping] =
    Seq(
      ParameterAnyShapeNode,
      ParameterNodeShapeNode,
      ParameterArrayShapeNode,
      ParameterUnionShapeNode,
      ParameterStringShapeNode,
      ParameterNumberShapeNode,
      ParameterFileShapeNode,
      ParameterNilShapeNode
    )

  val parametersDeclarationsIds: Seq[String] = parametersDeclarations.map(_.id)

}
