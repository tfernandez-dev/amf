package amf.maker

import amf.builder.{CreativeWorkBuilder, EndPointBuilder, LicenseBuilder, OrganizationBuilder}
import amf.common.ListAssertions
import amf.compiler.AMFCompiler
import amf.document.Document
import amf.domain.APIDocumentation
import amf.metadata.Field
import amf.metadata.domain.APIDocumentationModel._
import amf.remote.{AmfJsonLdHint, Hint, OasJsonHint, RamlYamlHint}
import amf.unsafe.PlatformSecrets
import org.scalatest.{Assertion, AsyncFunSuite}

import scala.concurrent.{ExecutionContext, Future}

class WebApiMakerTest extends AsyncFunSuite with PlatformSecrets with ListAssertions {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "file://shared/src/test/resources/maker/"

  test("generate complete web api instance") {

    val fixture = List(
      (Name, "test"),
      (Description, "testDescription"),
      (Host, "http://api.example.com/path"),
      (Schemes, List("http", "https")),
      (BasePath, "/path"),
      (ContentType, "application/yaml"),
      (Accepts, "application/yaml"),
      (Version, "1.1"),
      (TermsOfService, "terminos"),
      (Provider,
       OrganizationBuilder().withUrl("urlContacto").withName("nombreContacto").withEmail("mailContacto").build),
      (License, LicenseBuilder().withUrl("urlLicense").withName("nameLicense").build),
      (Documentation,
       CreativeWorkBuilder().withUrl("urlExternalDocs").withDescription("descriptionExternalDocs").build)
    )

    assertFixture(fixture, "completeExample.raml", RamlYamlHint)
  }

  test("WebApi with nested endpoints - RAML.") {
    val endpoints = List(
      EndPointBuilder()
        .withPath("/levelzero")
        .build,
      EndPointBuilder()
        .withPath("/levelzero/level-one")
        .withName("One display name")
        .withDescription("and this description!")
        .build,
      EndPointBuilder().withPath("/levelzero/another-level-one").withName("some other display name").build,
      EndPointBuilder().withPath("/another-levelzero").withName("Root name").build
    )
    val fixture = List(
      (Name, "API"),
      (BasePath, "/some/base/uri"),
      (EndPoints, endpoints)
    )

    assertFixture(fixture, "nested-endpoints.raml", RamlYamlHint)
  }

  test("WebApi with nested endpoints - OAS.") {
    val endpoints = List(
      EndPointBuilder()
        .withPath("/levelzero")
        .withName("Name")
        .build,
      EndPointBuilder()
        .withPath("/levelzero/level-one")
        .withName("One display name")
        .withDescription("and this description!")
        .build,
      EndPointBuilder().withPath("/levelzero/another-level-one").withName("some other display name").build,
      EndPointBuilder().withPath("/another-levelzero").withName("Root name").build
    )
    val fixture = List(
      (Name, "API"),
      (BasePath, "/some/base/uri"),
      (EndPoints, endpoints)
    )

    assertFixture(fixture, "nested-endpoints.json", OasJsonHint)
  }

  test("generate partial succeed") {
    val fixture = List(
      (Name, "test"),
      (Description, null),
      (Host, "http://api.example.com/path"),
      (Schemes, List("http", "https")),
      (BasePath, "/path"),
      (ContentType, "application/yaml"),
      (Accepts, "application/yaml"),
      (Version, "1.1"),
      (TermsOfService, null),
      (Provider,
       OrganizationBuilder().withUrl("urlContacto").withName("nombreContacto").withEmail("mailContacto").build),
      (Documentation,
       CreativeWorkBuilder().withUrl("urlExternalDocs").withDescription("descriptionExternalDocs").build)
    )

    assertFixture(fixture, "partialExample.raml", RamlYamlHint)
  }

  ignore("basic jsonld example") {
    val fixture = List(
      (Name, "test"),
      (Description, null),
      (Host, "api.example.com"),
      (Schemes, List("http", "https")),
      (BasePath, null),
      (TermsOfService, null),
      (Provider, null),
      (License, null),
      (Documentation, null)
    )

    assertFixture(fixture, "basicExample.jsonld", AmfJsonLdHint)
  }

  test("generate partial json") {

    val fixture = List(
      (Name, "test"),
      (Description, "testDescription"),
      (Host, "api.example.com"),
      (Schemes, List("http", "https")),
      (BasePath, "http://api.example.com/path"),
      (ContentType, "application/json"),
      (Accepts, "application/json"),
      (Version, "1.1"),
      (TermsOfService, "terminos"),
      (Provider, OrganizationBuilder().withUrl("urlContact").withName("nameContact").withEmail("emailContact").build),
      (License, LicenseBuilder().withUrl("urlLicense").withName("nameLicense").build),
      (Documentation,
       CreativeWorkBuilder().withUrl("urlExternalDocs").withDescription("descriptionExternalDocs").build)
    )

    assertFixture(fixture, "completeExample.json", OasJsonHint)
  }

  test("edit complete raml") {

    val before = List(
      (Name, "test"),
      (Description, "testDescription"),
      (Host, "http://api.example.com/path"),
      (Schemes, List("http", "https")),
      (BasePath, "/path"),
      (ContentType, "application/yaml"),
      (Accepts, "application/yaml"),
      (Version, "1.1"),
      (TermsOfService, "terminos"),
      (Provider,
       OrganizationBuilder().withUrl("urlContacto").withName("nombreContacto").withEmail("mailContacto").build),
      (License, LicenseBuilder().withUrl("urlLicense").withName("nameLicense").build),
      (Documentation,
       CreativeWorkBuilder().withUrl("urlExternalDocs").withDescription("descriptionExternalDocs").build)
    )

    val after = List(
      (Name, "test"),
      (Description, "changed"),
      (Host, "http://api.example.com/path"),
      (Schemes, List("http", "https")),
      (BasePath, "/path"),
      (ContentType, "application/yaml"),
      (Accepts, "application/yaml"),
      (Version, "1.1"),
      (TermsOfService, "terminos"),
      (Provider,
       OrganizationBuilder().withUrl("urlContacto").withName("nombreContacto").withEmail("mailContacto").build),
      (License, LicenseBuilder().withUrl("urlLicense").withName("changed").build),
      (Documentation,
       CreativeWorkBuilder().withUrl("urlExternalDocs").withDescription("descriptionExternalDocs").build)
    )

    AMFCompiler(basePath + "completeExample.raml", platform, RamlYamlHint)
      .build()
      .map { unit =>
        val api = unit.asInstanceOf[Document].encodes.asInstanceOf[APIDocumentation]
        assertWebApiValues(api, before)
        val builder = api.toBuilder
        builder.withDescription("changed")
        val license = api.license.toBuilder
        license.withName("changed")
        builder.withLicense(license.build)
        assertWebApiValues(builder.build, after)
      }
  }

  test("edit complete json") {
    val before = List(
      (Name, "test"),
      (Description, "testDescription"),
      (Host, "api.example.com"),
      (Schemes, List("http", "https")),
      (BasePath, "http://api.example.com/path"),
      (ContentType, "application/json"),
      (Accepts, "application/json"),
      (Version, "1.1"),
      (TermsOfService, "terminos"),
      (Provider, OrganizationBuilder().withUrl("urlContact").withName("nameContact").withEmail("emailContact").build),
      (License, LicenseBuilder().withUrl("urlLicense").withName("nameLicense").build),
      (Documentation,
       CreativeWorkBuilder().withUrl("urlExternalDocs").withDescription("descriptionExternalDocs").build)
    )

    val after = List(
      (Name, "test"),
      (Description, "changed"),
      (Host, "api.example.com"),
      (Schemes, List("http", "https")),
      (BasePath, "http://api.example.com/path"),
      (ContentType, "application/json"),
      (Accepts, "application/json"),
      (Version, "1.1"),
      (TermsOfService, "terminos"),
      (Provider, OrganizationBuilder().withUrl("urlContact").withName("nameContact").withEmail("emailContact").build),
      (License, LicenseBuilder().withUrl("urlLicense").withName("changed").build),
      (Documentation,
       CreativeWorkBuilder().withUrl("urlExternalDocs").withDescription("descriptionExternalDocs").build)
    )

    AMFCompiler(basePath + "completeExample.json", platform, OasJsonHint)
      .build()
      .map { unit =>
        val api = unit.asInstanceOf[Document].encodes.asInstanceOf[APIDocumentation]
        assertWebApiValues(api, before)
        val builder = api.toBuilder
        builder.withDescription("changed")
        val license = api.license.toBuilder
        license.withName("changed")
        builder.withLicense(license.build)
        assertWebApiValues(builder.build, after)
      }
  }

  def assertWebApiValues(api: APIDocumentation, assertions: List[(Field, Any)]): Assertion = {
    assertions.foreach {
      case (Name, expected)           => assertField(Name, api.name, expected)
      case (Description, expected)    => assertField(Description, api.description, expected)
      case (Host, expected)           => assertField(Host, api.host, expected)
      case (Schemes, expected)        => assertField(Schemes, api.schemes, expected)
      case (BasePath, expected)       => assertField(BasePath, api.basePath, expected)
      case (ContentType, expected)    => assertField(ContentType, api.contentType, expected)
      case (Accepts, expected)        => assertField(Accepts, api.accepts, expected)
      case (Version, expected)        => assertField(Version, api.version, expected)
      case (TermsOfService, expected) => assertField(TermsOfService, api.termsOfService, expected)
      case (Provider, expected)       => assertField(Provider, api.provider, expected)
      case (License, expected)        => assertField(License, api.license, expected)
      case (Documentation, expected)  => assertField(Documentation, api.documentation, expected)
      case (EndPoints, expected)      => assertField(EndPoints, api.endPoints, expected)
    }
    succeed
  }

  private def assertField(field: Field, actual: Any, expected: Any) =
    if (expected != actual) fail(s"Expected $expected but $actual found for field ${field.name}")

  private def assertFixture(fixture: List[(Field, Object)], file: String, hint: Hint): Future[Assertion] = {
    AMFCompiler(basePath + file, platform, hint)
      .build()
      .map { unit =>
        val api = unit.asInstanceOf[Document].encodes.asInstanceOf[APIDocumentation]
        assertWebApiValues(api, fixture)
      }
  }
}
