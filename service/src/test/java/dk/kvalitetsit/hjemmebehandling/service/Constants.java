package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.model.CPR;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;

import java.time.Instant;

public class Constants {
    public static final Instant POINT_IN_TIME = Instant.parse("2021-11-23T10:00:00.000Z");
    public static final QualifiedId.OrganizationId ORGANIZATION_ID_1 = new QualifiedId.OrganizationId("organization-1");
    public static final QualifiedId.OrganizationId ORGANIZATION_ID_2 = new QualifiedId.OrganizationId("organization-2");
    public static final String SOR_CODE_1 = "123456";
    public static final QualifiedId.CarePlanId CAREPLAN_ID_1 = new QualifiedId.CarePlanId("careplan-1");
    public static final QualifiedId.CarePlanId CAREPLAN_ID_2 = new QualifiedId.CarePlanId("careplan-2");
    public static final QualifiedId.CarePlanId CAREPLAN_ID_3 = new QualifiedId.CarePlanId("careplan-3");
    public static final CPR CPR_1 = new CPR("0101010101");
    public static final CPR CPR_2 = new CPR("0202020202");
    public static final QualifiedId.PatientId PATIENT_ID_1 = new QualifiedId.PatientId("patient-1");
    public static final QualifiedId.PatientId PATIENT_ID_2 = new QualifiedId.PatientId("patient-2");
    public static final QualifiedId.PlanDefinitionId PLANDEFINITION_ID_1 = new QualifiedId.PlanDefinitionId("plandefinition-1");
    public static final QualifiedId.PlanDefinitionId PLANDEFINITION_ID_2 = new QualifiedId.PlanDefinitionId("plandefinition-2");
    public static final QualifiedId.QuestionnaireId QUESTIONNAIRE_ID_1 = new QualifiedId.QuestionnaireId("questionnaire-1");
    public static final QualifiedId.QuestionnaireId QUESTIONNAIRE_ID_2 = new QualifiedId.QuestionnaireId("questionnaire-2");
    public static final QualifiedId.QuestionnaireId QUESTIONNAIRE_ID_3 = new QualifiedId.QuestionnaireId("questionnaire-3");
    public static final QualifiedId.QuestionnaireResponseId QUESTIONNAIRE_RESPONSE_ID_1 = new QualifiedId.QuestionnaireResponseId("questionnaireresponse-1");
    public static final QualifiedId.QuestionnaireResponseId QUESTIONNAIRE_RESPONSE_ID_2 = new QualifiedId.QuestionnaireResponseId("questionnaireresponse-2");
    public static final QualifiedId.QuestionnaireResponseId QUESTIONNAIRE_RESPONSE_ID_3 = new QualifiedId.QuestionnaireResponseId("questionnaireresponse-3");
    public static final String ORGANIZATION_NAME = "infektionsmedicinsk afdeling";
    public static final QualifiedId.PractitionerId PRACTITIONER_ID_1 = new QualifiedId.PractitionerId("practitioner-1");
    public static final String QUESTION_ID_1 = "question-1";
    public static final Pagination PAGINATION = new Pagination(0, 10);
}

