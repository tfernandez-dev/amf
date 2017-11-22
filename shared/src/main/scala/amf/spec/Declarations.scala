package amf.spec

import amf.common.core.QName
import amf.document.Fragment.Fragment
import amf.domain.Annotation.{DeclaredElement, SourceAST}
import amf.domain._
import amf.domain.`abstract`.{ResourceType, Trait}
import amf.domain.extensions.CustomDomainProperty
import amf.domain.security.SecurityScheme
import amf.model.AmfArray
import amf.shape.{NodeShape, Shape, UnresolvedShape}
import amf.spec.SearchScope.{All, Fragments, Named}
import org.yaml.model.YPart

/**
  * Declarations object.
  */
case class Declarations(var libraries: Map[String, Declarations] = Map(),
                        var fragments: Map[String, DomainElement] = Map(),
                        var shapes: Map[String, Shape] = Map(),
                        var annotations: Map[String, CustomDomainProperty] = Map(),
                        var resourceTypes: Map[String, ResourceType] = Map(),
                        var parameters: Map[String, Parameter] = Map(),
                        var payloads: Map[String, Payload] = Map(),
                        var traits: Map[String, Trait] = Map(),
                        var securitySchemes: Map[String, SecurityScheme] = Map(),
                        errorHandler: Option[ErrorHandler]) {

  def +=(fragment: (String, Fragment)): Declarations = {
    fragment match {
      case (url, f) => fragments = fragments + (url -> f.encodes)
    }
    this
  }

  def +=(element: DomainElement): Declarations = {
    element match {
      case r: ResourceType         => resourceTypes = resourceTypes + (r.name      -> r)
      case t: Trait                => traits = traits + (t.name                    -> t)
      case a: CustomDomainProperty => annotations = annotations + (a.name          -> a)
      case s: Shape                => shapes = shapes + (s.name                    -> s)
      case p: Parameter            => parameters = parameters + (p.name            -> p)
      case ss: SecurityScheme      => securitySchemes = securitySchemes + (ss.name -> ss)
    }
    this
  }

  /** Find domain element with the same name. */
  def findEquivalent(element: DomainElement): Option[DomainElement] = element match {
    case r: ResourceType         => findResourceType(r.name, SearchScope.All)
    case t: Trait                => findTrait(t.name, SearchScope.All)
    case a: CustomDomainProperty => findAnnotation(a.name, SearchScope.All)
    case s: Shape                => findType(s.name, SearchScope.All)
    case p: Parameter            => findParameter(p.name, SearchScope.All)
    case ss: SecurityScheme      => findSecurityScheme(ss.name, SearchScope.All)
    case _                       => None
  }

  def registerParameter(parameter: Parameter, payload: Payload): Unit = {
    parameters = parameters + (parameter.name -> parameter)
    payloads = payloads + (parameter.name     -> payload)
  }

  def parameterPayload(parameter: Parameter): Payload = payloads(parameter.name)

  /** Get or create specified library. */
  def getOrCreateLibrary(alias: String): Declarations = {
    libraries.get(alias) match {
      case Some(lib) => lib
      case None =>
        val result = Declarations(errorHandler = errorHandler)
        libraries = libraries + (alias -> result)
        result
    }
  }

  private def error(message: String, ast: Option[YPart]): Unit = errorHandler match {
    case Some(handler) => handler.violation(message, ast)
    case _             => throw new Exception(message)
  }

  private def error(message: String, ast: YPart): Unit = error(message, Option(ast))

  def declarables(): Seq[DomainElement] =
    (shapes.values ++ annotations.values ++ resourceTypes.values ++ traits.values ++ parameters.values ++ securitySchemes.values).toSeq

  def findParameterOrError(ast: YPart)(key: String, scope: SearchScope.Scope): Parameter =
    findParameter(key, scope) match {
      case Some(result) => result
      case _ =>
        error(s"Parameter '$key' not found", ast)
        ErrorParameter
    }

  def findParameter(key: String, scope: SearchScope.Scope): Option[Parameter] =
    findForType(key, _.parameters, scope) collect {
      case p: Parameter => p
    }

  def findResourceTypeOrError(ast: YPart)(key: String, scope: SearchScope.Scope): ResourceType =
    findResourceType(key, scope) match {
      case Some(result) => result
      case _ =>
        error(s"ResourceType $key not found", ast)
        ErrorResourceType
    }

  def findResourceType(key: String, scope: SearchScope.Scope): Option[ResourceType] =
    findForType(key, _.resourceTypes, scope) collect {
      case r: ResourceType => r
    }

  def findDocumentations(key: String, scope: SearchScope.Scope): Option[CreativeWork] =
    findForType(key, Map(), scope) collect {
      case u: CreativeWork => u
    }

  def findTraitOrError(ast: YPart)(key: String, scope: SearchScope.Scope): Trait = findTrait(key, scope) match {
    case Some(result) => result
    case _ =>
      error(s"Trait $key not found", ast)
      ErrorTrait
  }

  private def findTrait(key: String, scope: SearchScope.Scope): Option[Trait] =
    findForType(key, _.traits, scope) collect {
      case t: Trait => t
    }

  def findAnnotationOrError(ast: YPart)(key: String, scope: SearchScope.Scope): CustomDomainProperty =
    findAnnotation(key, scope) match {
      case Some(result) => result
      case _ =>
        error(s"Annotation '$key' not found", ast)
        ErrorCustomDomainProperty
    }

  def findAnnotation(key: String, scope: SearchScope.Scope): Option[CustomDomainProperty] =
    findForType(key, _.annotations, scope) collect {
      case a: CustomDomainProperty => a
    }

  def findType(key: String, scope: SearchScope.Scope): Option[Shape] = findForType(key, _.shapes, scope) collect {
    case s: Shape => s
  }

  def findSecuritySchemeOrError(ast: YPart)(key: String, scope: SearchScope.Scope): SecurityScheme =
    findSecurityScheme(key, scope) match {
      case Some(result) => result
      case _ =>
        error(s"SecurityScheme '$key' not found", ast)
        ErrorSecurityScheme
    }

  def findSecurityScheme(key: String, scope: SearchScope.Scope): Option[SecurityScheme] =
    findForType(key, _.securitySchemes, scope) collect {
      case ss: SecurityScheme => ss
    }

  def findNamedExampleOrError(ast: YPart)(key: String): Example = findNamedExample(key) match {
    case Some(result) => result
    case _ =>
      error(s"NamedExample '$key' not found", ast)
      ErrorNamedExample
  }

  def findNamedExample(key: String): Option[Example] = fragments.get(key) collect { case e: Example => e }

  /** Resolve all [[UnresolvedShape]] references or fail. */
  def resolve(): Unit = shapes.values.foreach(resolveShape)

  private def resolveShape(shape: Shape): Shape = {
    shape.fields.foreach {
      case (field, value) =>
        val resolved = value.value match {
          case u: UnresolvedShape => resolveOrError(u)
          case s: Shape           => resolveShape(s)
          case a: AmfArray =>
            AmfArray(a.values.map {
              case u: UnresolvedShape => resolveOrError(u)
              case s: Shape           => resolveShape(s)
              case o                  => o
            }, a.annotations)
          case o => o
        }

        shape.fields.setWithoutId(field, resolved, value.annotations)
    }
    shape
  }

  private def resolveOrError(unresolved: UnresolvedShape): Shape = shapes.get(unresolved.reference) match {
    case Some(target) => unresolved.resolve(target).add(DeclaredElement())
    case _ =>
      error(s"Could not resolve shape: ${unresolved.reference}",
            unresolved.annotations.find(classOf[SourceAST]).map(_.ast))
      NodeShape()
  }

  private def findForType(key: String,
                          map: Declarations => Map[String, DomainElement],
                          scope: SearchScope.Scope): Option[DomainElement] = {
    def inRef(): Option[DomainElement] = {
      val fqn = QName(key)

      val result = if (fqn.isQualified) {
        libraries.get(fqn.qualification).flatMap(_.findForType(fqn.name, map, scope))
      } else None

      result
        .orElse {
          map(this).get(key)
        }
    }

    scope match {
      case All       => inRef().orElse(fragments.get(key))
      case Fragments => fragments.get(key)
      case Named     => inRef()
    }
  }

  trait ErrorDeclaration

  object ErrorTrait                extends Trait(Fields(), Annotations()) with ErrorDeclaration
  object ErrorResourceType         extends ResourceType(Fields(), Annotations()) with ErrorDeclaration
  object ErrorCustomDomainProperty extends CustomDomainProperty(Fields(), Annotations()) with ErrorDeclaration
  object ErrorSecurityScheme       extends SecurityScheme(Fields(), Annotations()) with ErrorDeclaration
  object ErrorNamedExample         extends Example(Fields(), Annotations()) with ErrorDeclaration
  object ErrorCreativeWork         extends CreativeWork(Fields(), Annotations()) with ErrorDeclaration
  object ErrorParameter            extends Parameter(Fields(), Annotations()) with ErrorDeclaration

}

object Declarations {

  def apply(declarations: Seq[DomainElement], errorHandler: Option[ErrorHandler]): Declarations = {
    val result = Declarations(errorHandler = errorHandler)
    declarations.foreach(result += _)
    result
  }
}

object SearchScope {
  trait Scope

  object All       extends Scope
  object Fragments extends Scope
  object Named     extends Scope
}
