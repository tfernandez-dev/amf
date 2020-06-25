package amf.plugins.document.webapi.parser.spec.declaration

import amf.plugins.domain.shapes.models.TypeDef
import org.yaml.model.{YMap, YScalar}
import amf.core.parser._
import amf.plugins.document.webapi.contexts.WebApiContext
import amf.plugins.document.webapi.parser.OasTypeDefMatcher
import amf.plugins.domain.shapes.models.TypeDef._
import amf.validations.ParserSideValidations.InvalidJsonSchemaType

class OasTypeDetection(map: YMap)(implicit ctx: WebApiContext) {

  def detect(version: JSONSchemaVersion): TypeDef = {
    val defaultType = version match {
      case oasSchema: OASSchemaVersion if oasSchema.position == "parameter" => UndefinedType
      case _                                                                => AnyType
    }

    detectDependency()
      .orElse(detectType())
      .orElse(detectObjectProperties())
      .orElse(detectUnion())
      .orElse(detectItemProperties())
      .orElse(detectNumberProperties())
      .orElse(detectStringProperties())
      .getOrElse(defaultType)
  }

  private def detectObjectProperties(): Option[ObjectType.type] =
    map
      .key("properties")
      .orElse(map.key("x-amf-merge"))
      .orElse(map.key("minProperties"))
      .orElse(map.key("maxProperties"))
      .orElse(map.key("dependencies"))
      .orElse(map.key("patternProperties"))
      .orElse(map.key("additionalProperties"))
      .orElse(map.key("discriminator"))
      .map(_ => ObjectType)

  private def detectItemProperties(): Option[ArrayType.type] =
    map
      .key("items")
      .orElse(map.key("minItems"))
      .orElse(map.key("maxItems"))
      .orElse(map.key("uniqueItems"))
      .map(_ => ArrayType)

  private def detectNumberProperties(): Option[NumberType.type] =
    map
      .key("multipleOf")
      .orElse(map.key("minimum"))
      .orElse(map.key("maximum"))
      .orElse(map.key("exclusiveMinimum"))
      .orElse(map.key("exclusiveMaximum"))
      .map(_ => NumberType)

  private def detectStringProperties(): Option[StrType.type] =
    map
      .key("minLength")
      .orElse(map.key("maxLength"))
      .orElse(map.key("pattern"))
      .orElse(map.key("format"))
      .map(_ => StrType)

  private def detectDependency(): Option[TypeDef] = map.key("$ref").map(_ => LinkType)

  private def detectUnion(): Option[TypeDef.UnionType.type] = map.key("x-amf-union").map(_ => UnionType)

  private def detectType(): Option[TypeDef] = map.key("type").flatMap { e =>
    val t      = e.value.as[YScalar].text
    val f      = map.key("format").flatMap(e => e.value.toOption[YScalar].map(_.text)).getOrElse("")
    val result = OasTypeDefMatcher.matchType(t, f, UndefinedType)
    if (result == UndefinedType) {
      ctx.eh.violation(InvalidJsonSchemaType, "", s"Invalid type $t", e.value)
      None
    } else Some(result)
  }
}
