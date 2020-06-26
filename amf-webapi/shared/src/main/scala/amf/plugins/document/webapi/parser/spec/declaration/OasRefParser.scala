package amf.plugins.document.webapi.parser.spec.declaration

import amf.core.annotations.ExternalFragmentRef
import amf.core.model.domain.Shape
import amf.core.parser._
import amf.plugins.document.webapi.contexts.parser.OasLikeWebApiContext
import amf.plugins.document.webapi.contexts.parser.oas.{Oas2WebApiContext, Oas3WebApiContext}
import amf.plugins.document.webapi.parser.spec.OasDefinitions
import amf.plugins.document.webapi.parser.spec.declaration.utils.JsonSchemaParsingHelper
import amf.plugins.domain.shapes.models.{AnyShape, UnresolvedShape}
import org.yaml.model.{YMap, YMapEntry, YPart, YType}

class OasRefParser(map: YMap,
                   ast: YPart,
                   name: String,
                   adopt: Shape => Unit,
                   nameAnnotations: Annotations,
                   version: JSONSchemaVersion)(implicit ctx: OasLikeWebApiContext) {

  def parse(): Option[AnyShape] = {
    map
      .key("$ref")
      .flatMap { e =>
        e.value.tagType match {
          case YType.Null => Some(AnyShape(e))
          case _ =>
            findDeclarationAndParse(e)
        }
      }
  }

  private def findDeclarationAndParse(e: YMapEntry) = {
    val ref: String = e.value
    val text        = OasDefinitions.stripDefinitionsPrefix(ref)

    def createLinkToDeclaration(s: AnyShape) = {
      val link =
        s.link(text, Annotations(ast))
          .asInstanceOf[AnyShape]
          .withName(name, nameAnnotations)
          .withSupportsRecursion(true)
      adopt(link)
      Some(link)
    }

    ctx.declarations.findType(text, SearchScope.All) match { // normal declaration to be used from raml or oas
      case Some(s) => createLinkToDeclaration(s)
      case _       => // Only enabled for JSON Schema, not OAS. In OAS local references can only point to the #/definitions (#/components in OAS 3) node
        // now we work with canonical JSON schema pointers, not local refs
        val referencedShape = ctx.findLocalJsonNode(ref) match {
          case Some(_) => searchLocalJsonSchema(ref, if (!ctx.linkTypes) ref else text, e)
          case _       => searchRemoteJsonSchema(ref, if (!ctx.linkTypes) ref else text, e)
        }
        referencedShape.foreach(adopt(_))
        referencedShape
    }
  }

  private def searchLocalJsonSchema(r: String, t: String, e: YMapEntry): Option[AnyShape] = {
    val (ref, text) =
      if (ctx.linkTypes) (r, t)
      else {
        val fullref = ctx.resolvedPath(ctx.rootContextDocument, r)
        (fullref, fullref)
      }
    ctx.findJsonSchema(ref) match {
      case Some(s) =>
        val annots = Annotations(ast)
        val copied =
          s.link(ref, annots).asInstanceOf[AnyShape].withName(name, Annotations()).withSupportsRecursion(true)
        adopt(copied)
        Some(copied)
      // Local reference
      case None =>
        val tmpShape =
          UnresolvedShape(ref, map).withName(text, Annotations()).withSupportsRecursion(true)
        tmpShape.unresolved(text, e, "warning")(ctx)
        tmpShape.withContext(ctx)
        adopt(tmpShape)

        ctx match {
          case _ @(_: Oas2WebApiContext | _: Oas3WebApiContext) if isDeclaration(ref) =>
            val shape = AnyShape(ast).withName(name, nameAnnotations)
            shape.withLinkTarget(tmpShape).withLinkLabel(text)
            adopt(shape)
            Some(shape)
          case _ =>
            ctx.registerJsonSchema(ref, tmpShape)
            ctx.findLocalJsonNode(r) match {
              case Some(shapeNode) =>
                OasTypeParser(YMapEntry(name, shapeNode), adopt, version)
                  .parse()
                  .map { shape =>
                    ctx.futureDeclarations.resolveRef(text, shape)
                    //            tmpShape.resolve(shape) // useless?
                    ctx.registerJsonSchema(ref, shape)
                    if (ctx.linkTypes || ref.equals("#"))
                      shape.link(text, Annotations(ast)).asInstanceOf[AnyShape].withName(name, Annotations())
                    else shape
                  } orElse { Some(tmpShape) }

              case None =>
                //                          ctx.violation(tmpShape.id, s"Cannot find local JSON Schema reference $ref", e.value)
                Some(tmpShape)
            }
        }
    }
  }

  private val oas2DeclarationRegex = "^(\\#\\/definitions\\/){1}([^/\\n])+$"
  private val oas3DeclarationRegex =
    "^(\\#\\/components\\/){1}((schemas|parameters|securitySchemes|requestBodies|responses|headers|examples|links|callbacks){1}\\/){1}([^/\\n])+"
  private def isDeclaration(ref: String): Boolean =
    ctx match {
      case _: Oas2WebApiContext if ref.matches(oas2DeclarationRegex) => true
      case _: Oas3WebApiContext if ref.matches(oas3DeclarationRegex) => true
      case _                                                         => false
    }

  private def searchRemoteJsonSchema(ref: String, text: String, e: YMapEntry) = {
    val fullRef = ctx.resolvedPath(ctx.rootContextDocument, ref)
    ctx.findJsonSchema(fullRef).orElse(ctx.findJsonSchema(ref)) match {
      case Some(u: UnresolvedShape) => copyUnresolvedShape(ref, fullRef, e, u)
      case Some(shape)              => createLinkToParsedShape(ref, shape)
      case _ =>
        val fileUrl = ctx.jsonSchemaRefGuide.getFileUrl(ref)
        parseAndRegisterRemoteSchema(ref) match {
          case None =>
            val tmpShape = JsonSchemaParsingHelper.createTemporaryShape(shape => adopt(shape), e, ctx, fileUrl)
            // it might still be resolvable at the RAML (not JSON Schema) level
            tmpShape.unresolved(text, e, "warning").withSupportsRecursion(true)
            Some(tmpShape)
          case Some(jsonSchemaShape) =>
            if (ctx.declarations.fragments.contains(text)) {
              // case when in an OAS spec we point with a regular $ref to something that is external
              // and holds a JSON schema we need to promote an external fragment to data type fragment
              promoteParsedShape(ref, text, fullRef, jsonSchemaShape)
            } else {

              Some(jsonSchemaShape)
            }
        }
    }
  }

  private def copyUnresolvedShape(ref: String, fullRef: String, entry: YMapEntry, unresolved: UnresolvedShape) = {
    val annots = Annotations(ast)
    val copied = unresolved.copyShape(annots ++= unresolved.annotations.copy()).withLinkLabel(ref)
    copied.unresolved(fullRef, entry, "warning")(ctx)
    adopt(copied)
    Some(copied)
  }

  private def createLinkToParsedShape(ref: String, shape: AnyShape) = {
    val annots = Annotations(ast)
    val copied =
      shape.link(ref, annots).asInstanceOf[AnyShape].withName(name, nameAnnotations).withSupportsRecursion(true)
    adopt(copied)
    Some(copied)
  }

  private def promoteParsedShape(ref: String,
                                 text: String,
                                 fullRef: String,
                                 jsonSchemaShape: AnyShape): Option[AnyShape] = {
    val promotedShape =
      ctx.declarations.promoteExternaltoDataTypeFragment(text, fullRef, jsonSchemaShape)
    Some(
      promotedShape
        .link(text, Annotations(ast) += ExternalFragmentRef(ref))
        .asInstanceOf[AnyShape]
        .withName(name, nameAnnotations)
        .withSupportsRecursion(true))
  }

  private def parseAndRegisterRemoteSchema(fullRef: String): Option[AnyShape] = {
    ctx.parseRemoteJSONPath(fullRef).map { shape =>
      ctx.registerJsonSchema(fullRef, shape)
      ctx.futureDeclarations.resolveRef(fullRef, shape)
      shape
    }
  }
}
