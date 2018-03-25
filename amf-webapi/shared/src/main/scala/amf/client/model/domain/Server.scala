package amf.client.model.domain

import amf.client.convert.WebApiClientConverters._
import amf.client.model.StrField
import amf.plugins.domain.webapi.models.{Server => InternalServer}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

/**
  * Server model class.
  */
@JSExportAll
case class Server(override private[amf] val _internal: InternalServer) extends DomainElement {

  @JSExportTopLevel("model.domain.Server")
  def this() = this(InternalServer())

  def url: StrField                    = _internal.url
  def description: StrField            = _internal.description
  def variables: ClientList[Parameter] = _internal.variables.asClient

  /** Set url property of this Server. */
  def withUrl(url: String): this.type = {
    _internal.withUrl(url)
    this
  }

  /** Set description property of this Server. */
  def withDescription(description: String): this.type = {
    _internal.withDescription(description)
    this
  }

  /** Set host property of this Server. */
  def withVariables(variables: ClientList[Parameter]): this.type = {
    _internal.withVariables(variables.asInternal)
    this
  }

  /**
    * Adds one Parameter to the variables property of this Server and returns it for population.
    * name property of the Parameter is required.
    */
  def withVariable(name: String): Parameter = _internal.withVariable(name)
}
