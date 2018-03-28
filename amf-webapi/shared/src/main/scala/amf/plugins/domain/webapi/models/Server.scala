package amf.plugins.domain.webapi.models

import amf.client.model.StrField
import amf.core.metamodel.domain.DomainElementModel
import amf.core.model.domain.DomainElement
import amf.plugins.domain.webapi.metamodel.ServerModel._
import amf.core.parser.{Annotations, Fields}
import amf.plugins.domain.webapi.metamodel.ServerModel
import org.yaml.model.YMap
import amf.core.utils._

/**
  * Server internal model
  */
case class Server(fields: Fields, annotations: Annotations) extends DomainElement {

  def url: StrField             = fields.field(Url)
  def description: StrField     = fields.field(Description)
  def variables: Seq[Parameter] = fields.field(Variables)

  def withUrl(url: String): this.type                     = set(Url, url)
  def withDescription(description: String): this.type     = set(Description, description)
  def withVariables(variables: Seq[Parameter]): this.type = setArray(Variables, variables)

  def withVariable(name: String): Parameter = {
    val result = Parameter().withName(name)
    add(Variables, result)
    result
  }

  override def adopted(parent: String): this.type = withId(parent + "/" + url.option().map(_.urlEncoded).orNull)

  override def meta: DomainElementModel = ServerModel
}

object Server {

  def apply(): Server = apply(Annotations())

  def apply(ast: YMap): Server = apply(Annotations(ast))

  def apply(annotations: Annotations): Server = Server(Fields(), annotations)
}