package amf.plugins.domain.shapes.metamodel

import amf.core.metamodel.Field
import amf.core.metamodel.Type.Array
import amf.core.metamodel.domain._
import amf.core.vocabulary.Namespace._
import amf.core.vocabulary.{Namespace, ValueType}

/**
  *
  */
object ExamplesModel extends DomainElementModel with LinkableElementModel {

  val Examples = Field(
    Array(ExampleModel),
    ApiContract + "examples",
    ModelDoc(ModelVocabularies.ApiContract, "examples", "Examples list", Seq((Namespace.Core + "name").iri()))
  )

  override def fields: List[Field] =
    List(Examples) ++ DomainElementModel.fields ++ LinkableElementModel.fields

  override val `type`: List[ValueType] = ApiContract + "NamedExamples" :: DomainElementModel.`type`

  // override def modelInstance: ExamplesInstance = ExamplesInstance()

  override val doc: ModelDoc = ModelDoc(
    ModelVocabularies.ApiContract,
    "Examples",
    "Wrapper for a list of examples"
  )
}
