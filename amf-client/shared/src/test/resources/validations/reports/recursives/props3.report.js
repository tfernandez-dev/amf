Model: file://amf-client/shared/src/test/resources/validations/recursives/props3.raml
Profile: RAML 1.0
Conforms? false
Number of results: 3

Level: Violation

- Source: http://a.ml/vocabularies/amf/validation#example-validation-error
  Message: a should be string
b.a.a should be string
b.b should be string
c.b.a.a should be string
c.b.b should be string
c.c should be string

  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/recursives/props3.raml#/declarations/types/A/example/invalid
  Property: file://amf-client/shared/src/test/resources/validations/recursives/props3.raml#/declarations/types/A/example/invalid
  Position: Some(LexicalInformation([(23,0)-(34,0)]))
  Location: file://amf-client/shared/src/test/resources/validations/recursives/props3.raml

- Source: http://a.ml/vocabularies/amf/validation#example-validation-error
  Message: a.a should be string
a.c.b.a.a should be string
a.c.b.b should be string
a.c.c should be string
b should be string

  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/recursives/props3.raml#/declarations/types/B/example/invalid
  Property: file://amf-client/shared/src/test/resources/validations/recursives/props3.raml#/declarations/types/B/example/invalid
  Position: Some(LexicalInformation([(46,0)-(55,0)]))
  Location: file://amf-client/shared/src/test/resources/validations/recursives/props3.raml

- Source: http://a.ml/vocabularies/amf/validation#example-validation-error
  Message: b.b should be string
c should be string

  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/recursives/props3.raml#/declarations/types/C/example/invalid
  Property: file://amf-client/shared/src/test/resources/validations/recursives/props3.raml#/declarations/types/C/example/invalid
  Position: Some(LexicalInformation([(67,0)-(71,17)]))
  Location: file://amf-client/shared/src/test/resources/validations/recursives/props3.raml
