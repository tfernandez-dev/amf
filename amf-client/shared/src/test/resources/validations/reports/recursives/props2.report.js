Model: file://amf-client/shared/src/test/resources/validations/recursives/props2.raml
Profile: RAML 1.0
Conforms? false
Number of results: 3

Level: Violation

- Source: http://a.ml/vocabularies/amf/parser#example-validation-error
  Message: b.c.a.a should be string
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/recursives/props2.raml#/declarations/types/A/examples/example/invalid
  Property: 
  Position: Some(LexicalInformation([(18,0)-(25,0)]))
  Location: file://amf-client/shared/src/test/resources/validations/recursives/props2.raml

- Source: http://a.ml/vocabularies/amf/parser#example-validation-error
  Message: c.a.b.b should be string
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/recursives/props2.raml#/declarations/types/B/examples/example/invalid
  Property: 
  Position: Some(LexicalInformation([(39,0)-(46,0)]))
  Location: file://amf-client/shared/src/test/resources/validations/recursives/props2.raml

- Source: http://a.ml/vocabularies/amf/parser#example-validation-error
  Message: a.b.c.c should be string
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/recursives/props2.raml#/declarations/types/C/examples/example/invalid
  Property: 
  Position: Some(LexicalInformation([(60,0)-(66,18)]))
  Location: file://amf-client/shared/src/test/resources/validations/recursives/props2.raml
