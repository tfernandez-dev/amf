//package amf.plugins.document.webapi.parser.spec.common
//
//import amf.core.annotations.{ExplicitField, NilUnion}
//import amf.core.emitter.BaseEmitters._
//import amf.core.emitter._
//import amf.core.metamodel.Field
//import amf.core.metamodel.domain.ShapeModel
//import amf.core.metamodel.domain.extensions.PropertyShapeModel
//import amf.core.model.DataType
//import amf.core.model.domain._
//import amf.core.model.domain.extensions.PropertyShape
//import amf.core.parser.{ErrorHandler, FieldEntry, Position, Value}
//import amf.plugins.document.webapi.contexts.emitter.oas.{JsonSchemaEmitterContext, OasSpecEmitterContext}
//import amf.plugins.document.webapi.parser.JsonSchemaTypeDefMatcher
//import amf.plugins.document.webapi.parser.spec.OasDefinitions
//import amf.plugins.document.webapi.parser.spec.declaration.{DataNodeEmitter, OasCommonOASFieldsEmitter}
//import amf.plugins.domain.shapes.metamodel.AnyShapeModel._
//import amf.plugins.domain.shapes.metamodel.{ArrayShapeModel, NodeShapeModel, PropertyDependenciesModel, ScalarShapeModel}
//import amf.plugins.domain.shapes.models.TypeDef.UndefinedType
//import amf.plugins.domain.shapes.models._
//import amf.plugins.domain.shapes.parser.TypeDefXsdMapping
//import amf.plugins.domain.webapi.annotations.TypePropertyLexicalInfo
//import org.yaml.model.YDocument.{EntryBuilder, PartBuilder}
//import org.yaml.model.{YDocument, YNode, YScalar, YType}
//
//import scala.collection.immutable.ListMap
//import scala.collection.mutable
//import scala.collection.mutable.ListBuffer
//import org.mulesoft.lexer.Queue
//
//import scala.util.{Failure, Success, Try}
//
//class JsonSchemaEmitterJsonSchemaEmitter(options: ShapeRenderOptions) {
//
//  private val context = new JsonSchemaEmissionContext(simpleInheritanceFields =
//                                                        Seq(Examples, DisplayName, Name, Description),
//                                                      eh = options.errorHandler)
//
//  def emitShape(shape: AnyShape): String = {
//
//    val encodeEmitters: Seq[Emitter] = JsonSchemaTypeEmitter(shape)(context).emitters()
//    val declarationEmitters: ListBuffer[Emitter] = ListBuffer[Emitter]()
//    while (context.extractedDeclarations.nonEmpty()) {
//      val dec = context.extractedDeclarations.dequeue()
//      declarationEmitters ++ JsonSchemaTypeEmitter(dec)(context).emitters()
//    }
//
//    YDocument(b => {
//      b.obj { b =>
//        traverse(encodeEmitters, b)
//      }
//    })
//  }
//}
//
//case class JsonSchemaTypeEmitter(shape: Shape)(implicit ctx: JsonSchemaEmissionContext) {
//  def emitters(): Seq[Emitter] = {
//    shape match {
//      case l: Linkable if l.isLink      => Seq(new JsonSchemaRefEmitter(l))
//      case n: NodeShape                 => JsonSchemaNodeShapeEmitter(n).emitters()
//      case u: UnionShape if nilUnion(u) => JsonSchemaTypeEmitter(u.anyOf.head).emitters()
//      case u: UnionShape                => JsonSchemaUnionShapeEmitter(u).emitters()
//      case da: DataArrangementShape     => JsonSchemaDataArrangement(da).emitters()
//      case n: NilShape                  => Seq(JsonSchemaNilShapeEmitter(n))
//      case f: FileShape                 => JsonSchemaFileShapeEmitter(f).emitters()
//      case s: ScalarShape               => JsonSchemaScalarShapeEmitter(s).emitters()
//      case r: RecursiveShape            => JsonSchemaRecursiveShapeEmitter(r).emitters()
//      case _                            => Seq()
//    }
//  }
//
//  def entries(): Seq[EntryEmitter] = emitters() collect { case e: EntryEmitter => e }
//
//  def nilUnion(union: UnionShape): Boolean =
//    union.anyOf.size == 1 && union.anyOf.head.annotations.contains(classOf[NilUnion])
//}
//
//case class JsonSchemaTypePartEmitter(shape: Shape)(implicit ctx: JsonSchemaEmissionContext)
//    extends JsonSchemaPartTypeCollector(shape)
//    with PartEmitter {
//
//  override def emit(b: PartBuilder): Unit =
//    emitter() match {
//      case Left(p)        => p.emit(b)
//      case Right(entries) => b.obj(traverse(entries, _))
//    }
//
//  override def position(): Position = Position.ZERO
//}
//
//abstract class JsonSchemaShapeEmitter(s: Shape)(implicit ctx: JsonSchemaEmissionContext) {
//  def emitters(): Seq[EntryEmitter] = {
//    s.fields.entry(ShapeModel.Values).map(f => JsonSchemaEnumEmitter("enum", f.value)) match {
//      case Some(e) => Seq(e)
//      case _       => Nil
//    }
//  }
//}
//
//case class JsonSchemaRefEmitter(value: String)(implicit ctx: JsonSchemaEmissionContext) extends PartEmitter {
//
//  def this(linkable: Linkable) = this(linkable.linkLabel.option().getOrElse(linkable.id))
//
//  override def emit(b: YDocument.PartBuilder): Unit = {
//    val url = OasDefinitions.appendDefinitionsPrefix(value)
//    b.obj(MapEntryEmitter("$ref", url).emit(_))
//  }
//
//  override def position(): Position = Position.ZERO
//}
//
//case class JsonSchemaNodeShapeEmitter(node: NodeShape)(implicit ctx: JsonSchemaEmissionContext)
//    extends JsonSchemaShapeEmitter(node) {
//  override def emitters(): Seq[EntryEmitter] = {
//
//
//    val result: ListBuffer[EntryEmitter] = ListBuffer(super.emitters(): _*)
//
//    val fs = node.fields
//
//    result += MapEntryEmitter("type", "object")
//
//    fs.entry(NodeShapeModel.MinProperties).foreach(f => result += ValueEmitter("minProperties", f))
//
//    fs.entry(NodeShapeModel.MaxProperties).foreach(f => result += ValueEmitter("maxProperties", f))
//
//    fs.entry(NodeShapeModel.Closed)
//      .filter(f => f.value.annotations.contains(classOf[ExplicitField]) || f.scalar.toBool) match {
//      case Some(f) => result += ValueEmitter("additionalProperties", f.negated)
//      case _ =>
//        fs.entry(NodeShapeModel.AdditionalPropertiesSchema)
//          .foreach(f => result += JsonSchemaEntryShapeEmitter("additionalProperties", f))
//    }
//
//    fs.entry(NodeShapeModel.Properties).foreach(f => result += JsonSchemaPropertiesShapeEmitter(f))
//
//    val properties = ListMap(node.properties.map(p => p.id -> p): _*)
//
//    fs.entry(NodeShapeModel.Dependencies).map(f => result += JsonSchemaDependenciesEmitter(f, properties))
//
//    val shape = JsonSchemaEmitterHelper.resolveInheritance(node, ctx)
////    fs.entry(NodeShapeModel.Inherits).map(f => result += JsonSchemaInheritsEmitter(f))
//
//    result
//
//  }
//}
//
//case class JsonSchemaDependenciesEmitter(f: FieldEntry, propertiesMap: ListMap[String, PropertyShape])(
//    implicit ctx: JsonSchemaEmissionContext)
//    extends EntryEmitter {
//  override def emit(b: EntryBuilder): Unit = {
//
//    b.entry(
//      "dependencies",
//      _.obj { b =>
//        val result = f.array.values.map(v =>
//          JsonSchemaPropertyDependenciesEmitter(v.asInstanceOf[PropertyDependencies], propertiesMap))
//        traverse(result, b)
//      }
//    )
//  }
//
//  override def position(): Position = pos(f.value.annotations)
//}
//
//case class JsonSchemaPropertyDependenciesEmitter(
//    property: PropertyDependencies,
//    properties: ListMap[String, PropertyShape])(implicit ctx: JsonSchemaEmissionContext)
//    extends EntryEmitter {
//
//  override def emit(b: EntryBuilder): Unit = {
//    properties
//      .get(property.propertySource.value())
//      .foreach(p => {
//        b.entry(
//          p.name.value(),
//          _.list { b =>
//            val targets = property.fields
//              .entry(PropertyDependenciesModel.PropertyTarget)
//              .map(f => {
//                f.array.scalars.flatMap(iri =>
//                  properties.get(iri.value.toString).map(p => AmfScalar(p.name.value(), iri.annotations)))
//              })
//
//            targets.foreach(target => {
//              traverse(target.map(t => ScalarEmitter(t)), b)
//            })
//          }
//        )
//      })
//  }
//
//  override def position(): Position = pos(property.annotations) // TODO check this
//}
//
//case class JsonSchemaPropertiesShapeEmitter(f: FieldEntry)(implicit ctx: JsonSchemaEmissionContext)
//    extends EntryEmitter {
//  override def emit(b: EntryBuilder): Unit = {
//    val properties = f.array.values.partition(_.asInstanceOf[PropertyShape].patternName.option().isDefined)
//
//    // If properties not empty, emit it
//    properties._2 match {
//      case Nil  =>
//      case some => emitProperties(some, "properties", b)
//    }
//
//    // If patternProperties not empty, emit it
//    properties._1 match {
//      case Nil  =>
//      case some => emitProperties(some, "patternProperties", b)
//    }
//  }
//
//  override def position(): Position = pos(f.value.annotations)
//
//  private def emitProperties(properties: Seq[AmfElement], propertiesKey: String, b: EntryBuilder) {
//    b.entry(
//      propertiesKey,
//      _.obj { b =>
//        val result =
//          properties.map(v => JsonSchemaPropertyShapeEmitter(v.asInstanceOf[PropertyShape], propertiesKey))
//        traverse(result, b)
//      }
//    )
//  }
//}
//
//case class JsonSchemaPropertyShapeEmitter(property: PropertyShape, propertiesKey: String)(
//    implicit ctx: JsonSchemaEmissionContext)
//    extends JsonSchemaPartTypeCollector(property.range)
//    with EntryEmitter {
//
//  val readOnlyEmitter: Option[ValueEmitter] =
//    property.fields.entry(PropertyShapeModel.ReadOnly).map(fe => ValueEmitter("readOnly", fe))
//
//  val propertyName: String = property.patternName.option().getOrElse(property.name.value())
//  val propertyKey          = YNode(YScalar(propertyName), YType.Str)
//
//  override def emit(b: EntryBuilder): Unit = {
//    property.range match {
//      case _: AnyShape | _: RecursiveShape =>
//        b.entry(
//          propertyKey,
//          pb => {
//            emitter() match {
//              case Left(p)        => p.emit(pb)
//              case Right(entries) => pb.obj(traverse(entries ++ readOnlyEmitter, _))
//            }
//          }
//        )
//      case _ => // ignore
//        b.entry(propertyKey, _.obj(e => traverse(readOnlyEmitter.toSeq, e)))
//    }
//  }
//
//  override def position(): Position = pos(property.annotations) // TODO check this
//}
//
//abstract class JsonSchemaPartTypeCollector(shape: Shape)(implicit ctx: JsonSchemaEmissionContext) {
//  def emitter(): Either[PartEmitter, Seq[EntryEmitter]] = {
//    JsonSchemaTypeEmitter(shape).emitters() match {
//      case Seq(p: PartEmitter)                           => Left(p)
//      case es if es.forall(_.isInstanceOf[EntryEmitter]) => Right(es.collect { case e: EntryEmitter => e })
//      case other                                         => throw new Exception(s"IllegalTypeDeclarations found: $other")
//    }
//  }
//}
//
//case class JsonSchemaEnumEmitter(key: String, value: Value)(implicit ctx: JsonSchemaEmissionContext)
//    extends EntryEmitter {
//  override def emit(b: EntryBuilder): Unit = {
//    val nodes        = value.value.asInstanceOf[AmfArray].values.asInstanceOf[Seq[DataNode]]
//    val nodeEmitters = nodes.map(DataNodeEmitter(_, SpecOrdering.Lexical)(ctx.eh))
//    b.entry(key, _.list(traverse(SpecOrdering.Lexical.sorted(nodeEmitters), _)))
//  }
//
//  override def position(): Position = pos(value.annotations)
//}
//
//case class JsonSchemaEntryShapeEmitter(key: String, f: FieldEntry)(implicit ctx: JsonSchemaEmissionContext)
//    extends EntryEmitter {
//  override def emit(b: EntryBuilder): Unit = {
//    b.entry(
//      key,
//      _.obj { b =>
//        val emitters = JsonSchemaTypeEmitter(f.element.asInstanceOf[Shape]).entries()
//        traverse(SpecOrdering.Lexical.sorted(emitters), b)
//      }
//    )
//  }
//
//  override def position(): Position = pos(f.value.annotations)
//}
//
//case class JsonSchemaUnionShapeEmitter(union: UnionShape)(implicit ctx: JsonSchemaEmissionContext)
//    extends JsonSchemaShapeEmitter(union) {
//  override def emitters(): Seq[EntryEmitter] = super.emitters() :+ JsonSchemaAnyOfShapeEmitter(union)
//}
//
//case class JsonSchemaAnyOfShapeEmitter(shape: UnionShape)(implicit ctx: JsonSchemaEmissionContext)
//    extends EntryEmitter {
//
//  override def emit(b: EntryBuilder): Unit = {
//    b.entry(
//      "anyOf",
//      _.list { b =>
//        val emitters = shape.anyOf.map(JsonSchemaTypePartEmitter)
//        traverse(emitters, b)
//      }
//    )
//  }
//
//  override def position(): Position = pos(shape.annotations)
//}
//
//case class JsonSchemaDataArrangement(shape: DataArrangementShape)(implicit ctx: JsonSchemaEmissionContext)
//    extends JsonSchemaShapeEmitter(shape) {
//  override def emitters(): Seq[EntryEmitter] = {
//    val result = ListBuffer[EntryEmitter](super.emitters(): _*)
//    val fs     = shape.fields
//
//    result += MapEntryEmitter("type", "array")
//
//    fs.entry(ArrayShapeModel.MaxItems).map(f => result += ValueEmitter("maxItems", f))
//
//    fs.entry(ArrayShapeModel.MinItems).map(f => result += ValueEmitter("minItems", f))
//
//    fs.entry(ArrayShapeModel.UniqueItems).map(f => result += ValueEmitter("uniqueItems", f))
//
////    fs.entry(NodeShapeModel.Inherits).map(f => result += JsonSchemaInherits(f))
//
//    result
//  }
//}
//
//case class JsonSchemaNilShapeEmitter(nil: NilShape) extends EntryEmitter {
//  override def emit(b: EntryBuilder): Unit = b.entry("type", "null")
//
//  override def position(): Position = pos(nil.annotations)
//}
//
//case class JsonSchemaFileShapeEmitter(file: FileShape)(implicit ctx: JsonSchemaEmissionContext) {
//  def emitters(): Seq[EntryEmitter] = {
//    // In JSON-SCHEMA the datatype file is not valid, so we 'convert it' in a string scalar
//    val scalar = ScalarShape
//      .apply(file.fields, file.annotations)
//      .withDataType(DataType.String)
//      .copy(fields = file.fields)
//    JsonSchemaScalarShapeEmitter(scalar).emitters()
//  }
//}
//
//case class JsonSchemaScalarShapeEmitter(scalar: ScalarShape)(implicit ctx: JsonSchemaEmissionContext)
//    extends JsonSchemaShapeEmitter(scalar)
//    with OasCommonOASFieldsEmitter {
//
//  override implicit val spec: OasSpecEmitterContext = JsonSchemaEmitterContext(eh = ctx.eh)
//  override def typeDef: Option[TypeDef]             = scalar.dataType.option().map(TypeDefXsdMapping.typeDef)
//
//  override def emitters(): Seq[EntryEmitter] = {
//
//    val result: ListBuffer[EntryEmitter] = ListBuffer(super.emitters(): _*)
//    val fs                               = scalar.fields
//
//    fs.entry(ScalarShapeModel.DataType)
//      .foreach { f =>
//        val typeDefStr = JsonSchemaTypeDefMatcher.matchType(typeDef.get)
//        scalar.annotations.find(classOf[TypePropertyLexicalInfo]) match {
//          case Some(lexicalInfo) =>
//            result += MapEntryEmitter("type", typeDefStr, YType.Str, lexicalInfo.range.start)
//          case _ =>
//            result += MapEntryEmitter("type", typeDefStr, position = pos(f.value.annotations)) // TODO check this  - annotations of typeDef in parser
//        }
//      }
//
//    fs.entry(ScalarShapeModel.Format) match {
//      case Some(_) => // ignore, this will be set with the explicit information
//      case None =>
//        spec.typeDefMatcher.matchFormat(typeDef.getOrElse(UndefinedType)) match {
//          case Some(format) =>
//            result += RawValueEmitter("format", ScalarShapeModel.Format, checkRamlFormats(format))
//          case None => // ignore
//        }
//    }
//
//    emitCommonFields(fs, result)
//
//    result
//  }
//}
//
//case class JsonSchemaRecursiveShapeEmitter(recursive: RecursiveShape)(implicit ctx: JsonSchemaEmissionContext) {
//  def emitters(): Seq[JsonSchemaRefEmitter] =
//    recursive.fixpointTarget.flatMap(_.name.option()).map(JsonSchemaRefEmitter).toSeq
//}
//
//case class JsonSchemaDeclarationsEmitter(declarations: Seq[AnyShape]) {}
//
//object JsonSchemaEmitterHelper {
//  def resolveInheritance(shape: Shape, ctx: JsonSchemaEmissionContext): Shape = {
//    // TODO: evaluate if it is simple inheritanse or not
//    // If is simple, return the
//
//    shape.inherits.head
//  }
//}
//
//class JsonSchemaEmissionContext(val extractedDeclarations: ShapeQueue = ShapeQueue(),
//                                val simpleInheritanceFields: Seq[Field],
//                                val eh: ErrorHandler)
//
//case class ShapeQueue() {
//
//  private val queue: Queue[Shape]                = new Queue()
//  private val queuedIds: mutable.HashSet[String] = mutable.HashSet[String]()
//
//  def +=(s: Shape): Try[Unit] = {
//    if (queuedIds.contains(s.id)) Failure(new IllegalArgumentException("ID already queued"))
//    else {
//      queue.+=(s)
//      queuedIds+=(s.id)
//      Success()
//    }
//  }
//
//  def dequeue(): Shape = queue.dequeue
//
//  def nonEmpty(): Boolean = !queue.isEmpty
//}
