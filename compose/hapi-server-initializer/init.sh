#!/bin/sh

echo 'Installing curl ...';
apk add curl;

echo 'Waiting for hapi-server to be ready ...';
curl -o /dev/null --retry 5 --retry-max-time 40 --retry-connrefused http://hapi-server:8080

echo 'Initializing hapi-server ...';

function delete {
  echo 'Deleting '$1' ...'

  if [ $(curl -s -o /dev/null -w '%{http_code}' -X DELETE 'http://hapi-server:8080/fhir/'$1) -ne '200' ]
  then
    echo 'error deleting object, exiting ...'
    exit 1
  else
    echo 'successfully deleted '$1'!'
  fi
}

function create {
  echo 'Creating '$2' ...'

  # Using PUT allows us to control the resource id's.
  if [ $(curl -s -o /dev/null -w '%{http_code}' -X PUT -d '@/hapi-server-initializer/'$1 -H 'Content-Type: application/fhir+xml' 'http://hapi-server:8080/fhir/'$2'?_format=xml') -ne '201' ]
  then
    echo 'error creating object, exiting ...'
    exit 1
  else
      echo 'successfully created '$2'!'
  fi
}

#delete 'SearchParameter/searchparameter-examination-status'
#
#delete 'QuestionnaireResponse/questionnaireresponse-3'
#delete 'QuestionnaireResponse/questionnaireresponse-2'
#delete 'QuestionnaireResponse/questionnaireresponse-1'
#
#delete 'CarePlan/careplan-1'
#
#delete 'PlanDefinition/plandefinition-1'
#
#delete 'Questionnaire/questionnaire-2'
#delete 'Questionnaire/questionnaire-1'
#
#delete 'Patient/patient-2'
#delete 'Patient/patient-1'

create 'patient-1.xml' 'Patient/patient-1'
create 'patient-2.xml' 'Patient/patient-2'

create 'questionnaire-1.xml' 'Questionnaire/questionnaire-1'
create 'questionnaire-2.xml' 'Questionnaire/questionnaire-2'

create 'plandefinition-1.xml' 'PlanDefinition/plandefinition-1'

create 'careplan-1.xml' 'CarePlan/careplan-1'

create 'questionnaireresponse-1.xml' 'QuestionnaireResponse/questionnaireresponse-1'
create 'questionnaireresponse-2.xml' 'QuestionnaireResponse/questionnaireresponse-2'
create 'questionnaireresponse-3.xml' 'QuestionnaireResponse/questionnaireresponse-3'

create 'searchparameter-examination-status.xml' 'SearchParameter/searchparameter-examination-status'

echo 'Done initializing hapi-server!';