package amf.resolution

import amf.compiler.CompilerTestBuilder
import amf.core.model.document.BaseUnit
import amf.core.model.domain.Shape
import amf.core.parser.ParserContext
import amf.core.remote.{Raml, RamlYamlHint}
import amf.core.services.RuntimeValidator
import amf.core.vocabulary.Namespace
import amf.facades.Validation
import amf.io.BuildCycleTests
import amf.plugins.document.webapi.RAML10Plugin
import amf.plugins.document.webapi.contexts.Raml10WebApiContext
import amf.plugins.document.webapi.parser.spec.raml.RamlTypeExpressionParser
import amf.plugins.domain.shapes.models._
import org.scalatest.Matchers._

import scala.util.{Failure, Success}

class TypeResolutionTest extends BuildCycleTests with CompilerTestBuilder {

  test("TypeExpressions") {
    Validation(platform)
      .map(_.withEnabledValidation(false))
      .map(_ => {
        val adopt = (shape: Shape) => { shape.adopted("/test") }

        implicit val ctx: Raml10WebApiContext = new Raml10WebApiContext("", Nil, ParserContext())

        var res = RamlTypeExpressionParser(adopt).parse("integer")
        assert(res.get.isInstanceOf[ScalarShape])
        assert(res.get.asInstanceOf[ScalarShape].dataType.is((Namespace.Xsd + "integer").iri()))

        res = RamlTypeExpressionParser(adopt).parse("(integer)")
        assert(res.get.isInstanceOf[ScalarShape])
        assert(res.get.asInstanceOf[ScalarShape].dataType.is((Namespace.Xsd + "integer").iri()))

        res = RamlTypeExpressionParser(adopt).parse("((integer))")
        assert(res.get.isInstanceOf[ScalarShape])
        assert(res.get.asInstanceOf[ScalarShape].dataType.is((Namespace.Xsd + "integer").iri()))

        res = RamlTypeExpressionParser(adopt).parse("integer[]")
        assert(res.get.isInstanceOf[ArrayShape])
        assert(
          res.get
            .asInstanceOf[ArrayShape]
            .items
            .asInstanceOf[ScalarShape]
            .dataType
            .is((Namespace.Xsd + "integer").iri()))
        assert(res != null)

        res = RamlTypeExpressionParser(adopt).parse("(integer)[]")
        assert(res.get.isInstanceOf[ArrayShape])
        assert(
          res.get
            .asInstanceOf[ArrayShape]
            .items
            .asInstanceOf[ScalarShape]
            .dataType
            .is((Namespace.Xsd + "integer").iri()))
        assert(res != null)

        var error = false
        try {
          RuntimeValidator.disableValidations() { () =>
            val fail = new Raml10WebApiContext("", Nil, ctx)
            RamlTypeExpressionParser(adopt)(fail).parse("[]")
          }
        } catch {
          case e: Exception => error = true
        }
        assert(error)

        res = RamlTypeExpressionParser(adopt).parse("integer | string")
        assert(res.get.isInstanceOf[UnionShape])
        var union = res.get.asInstanceOf[UnionShape]
        assert(union.anyOf.length == 2)
        assert(union.anyOf.map { e =>
          e.asInstanceOf[ScalarShape].dataType.value()
        } == Seq((Namespace.Xsd + "integer").iri(), (Namespace.Xsd + "string").iri()))
        assert(res != null)

        res = RamlTypeExpressionParser(adopt).parse("(integer )| (string)")
        assert(res.get.isInstanceOf[UnionShape])
        union = res.get.asInstanceOf[UnionShape]
        assert(union.anyOf.length == 2)
        assert(union.anyOf.map { e =>
          e.asInstanceOf[ScalarShape].dataType.value()
        } == Seq((Namespace.Xsd + "integer").iri(), (Namespace.Xsd + "string").iri()))
        assert(res != null)

        res = RamlTypeExpressionParser(adopt).parse("(integer | string) | number")
        assert(res.get.isInstanceOf[UnionShape])
        union = res.get.asInstanceOf[UnionShape]
        assert(union.anyOf.length == 3)
        assert(union.anyOf.map { e =>
          e.asInstanceOf[ScalarShape].dataType.value()
        } == Seq((Namespace.Xsd + "integer").iri(),
                 (Namespace.Xsd + "string").iri(),
                 (Namespace.Shapes + "number").iri()))
        assert(res != null)

        res = RamlTypeExpressionParser(adopt).parse("(integer | string)[]")
        assert(res.get.isInstanceOf[ArrayShape])
        var array = res.get.asInstanceOf[ArrayShape]
        assert(array.items.isInstanceOf[UnionShape])
        union = array.items.asInstanceOf[UnionShape]
        assert(union.anyOf.map { e =>
          e.asInstanceOf[ScalarShape].dataType.value()
        } == Seq((Namespace.Xsd + "integer").iri(), (Namespace.Xsd + "string").iri()))
        assert(res != null)

        res = RamlTypeExpressionParser(adopt).parse("(integer | string[])")
        assert(res != null)
        assert(res.get.isInstanceOf[UnionShape])
        union = res.get.asInstanceOf[UnionShape]
        assert(union.anyOf.length == 2)
        assert(union.anyOf.head.isInstanceOf[ScalarShape])
        assert(union.anyOf.head.asInstanceOf[ScalarShape].dataType.is((Namespace.Xsd + "integer").iri()))
        assert(union.anyOf.last.isInstanceOf[ArrayShape])
        assert(
          union.anyOf.last
            .asInstanceOf[ArrayShape]
            .items
            .asInstanceOf[ScalarShape]
            .dataType
            .is((Namespace.Xsd + "string").iri()))

        val caught = intercept[Exception] { // Result type: Assertion
          res = RamlTypeExpressionParser(adopt).parse("[]string")
          assert(res != null)
          assert(res.get.isInstanceOf[UnionShape])
          union = res.get.asInstanceOf[UnionShape]
          assert(union.anyOf.length == 2)
          assert(union.anyOf.head.isInstanceOf[ScalarShape])
          assert(union.anyOf.head.asInstanceOf[ScalarShape].dataType.is((Namespace.Xsd + "integer").iri()))
          assert(union.anyOf.last.isInstanceOf[ArrayShape])
          assert(
            union.anyOf.last
              .asInstanceOf[ArrayShape]
              .items
              .asInstanceOf[ScalarShape]
              .dataType
              .is((Namespace.Xsd + "string").iri()))
        }
        assert(caught.getMessage.contains("Error parsing type expression, cannot accept type ScalarShape"))

        res = RamlTypeExpressionParser(adopt).parse("integer | string[]")
        assert(res != null)
        assert(res.get.isInstanceOf[UnionShape])
        union = res.get.asInstanceOf[UnionShape]
        assert(union.anyOf.length == 2)
        assert(union.anyOf.head.isInstanceOf[ScalarShape])
        assert(union.anyOf.head.asInstanceOf[ScalarShape].dataType.is((Namespace.Xsd + "integer").iri()))
        assert(union.anyOf.last.isInstanceOf[ArrayShape])
        assert(
          union.anyOf.last
            .asInstanceOf[ArrayShape]
            .items
            .asInstanceOf[ScalarShape]
            .dataType
            .is((Namespace.Xsd + "string").iri()))

        res = RamlTypeExpressionParser(adopt).parse("integer[][]")
        assert(res != null)
        assert(res.get.isInstanceOf[MatrixShape])
        var matrix = res.get.asInstanceOf[MatrixShape]
        assert(matrix.items.isInstanceOf[ArrayShape])
        array = matrix.items.asInstanceOf[ArrayShape]
        assert(array.items.isInstanceOf[ScalarShape])
        assert(array.items.asInstanceOf[ScalarShape].dataType.is((Namespace.Xsd + "integer").iri()))
      })
  }

  override val basePath = "amf-client/shared/src/test/resources/resolution/"

  val examples = Seq(
    "union1",
    "union2",
    "union3",
    "union4",
    "union5",
    "union6",
    "union7",
    "union-with-examples",
    "inheritance1",
    "inheritance2",
    "inheritance3",
    "inheritance4",
    "inheritance5",
    "inheritance6",
    "inheritance7",
    "inheritance8",
    "array_inheritance1",
    "array_inheritance2",
    "array_inheritance3",
    "complex_example1",
    "shape1",
    "shape2",
    "shape3",
    "xmlschema1"
  )

  examples.foreach { example =>
    test(s"Resolve data types: $example") {
      Validation(platform)
        .flatMap(v => {
          v.withEnabledValidation(false)
          v.loadValidationDialect().map(_ => v)
        })
        .flatMap { validation =>
          cycle(s"$example.raml", s"${example}_canonical.raml", RamlYamlHint, Raml, basePath, Some(validation))
        }
    }
  }

  override def transform(unit: BaseUnit, config: CycleConfig): BaseUnit =
    RAML10Plugin.resolve(unit)

  val errorExamples = Seq(
    "inheritance_error1",
    "inheritance_error2",
    "inheritance_error3"
  )

  errorExamples.foreach { example =>
    test(s"Fails on erroneous data types: $example") {
      build(s"file://$basePath$example.raml", RamlYamlHint)
        .map(u => RAML10Plugin.resolve(u))
        .transformWith {
          case Success(_) =>
            fail("Expected resolution error")
            succeed
          case Failure(exception) =>
            exception.getMessage should startWith("Resolution error:")
            succeed
        }
    }
  }
}