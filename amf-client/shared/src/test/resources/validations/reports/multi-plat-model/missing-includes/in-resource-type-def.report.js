Model: file://amf-client/shared/src/test/resources/validations/missing-includes/in-resource-type-def.raml
Profile: RAML
Conforms? false
Number of results: 2

Level: Violation

- Source: http://a.ml/vocabularies/amf/parser#parsing-error
  Message: ResourceType resourceTypes/idReturned.raml not found
  Level: Violation
  Target: 
  Property: 
  Position: Some(LexicalInformation([(5,16)-(5,54)]))
  Location: file://amf-client/shared/src/test/resources/validations/missing-includes/in-resource-type-def.raml

- Source: http://a.ml/vocabularies/amf/parser#parsing-error
  Message: java.io.IOException: ENOENT: no such file or directory, open 'amf-client/shared/src/test/resources/validations/missing-includes/resourceTypes/idReturned.raml'
  Level: Violation
  Target: resourceTypes/idReturned.raml
  Property: 
  Position: Some(LexicalInformation([(5,16)-(5,54)]))
  Location: file://amf-client/shared/src/test/resources/validations/missing-includes/in-resource-type-def.raml