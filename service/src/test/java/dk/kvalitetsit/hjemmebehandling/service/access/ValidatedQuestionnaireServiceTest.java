package dk.kvalitetsit.hjemmebehandling.service.access;

import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.repository.CarePlanRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcreteCarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcreteQuestionnaireResponseService;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcreteQuestionnaireService;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidatedQuestionnaireServiceTest {

    private static final QualifiedId.CarePlanId CAREPLAN_ID_1 = new QualifiedId.CarePlanId("careplan-1");
    private static final QualifiedId.QuestionnaireId QUESTIONNAIRE_ID_1 = new QualifiedId.QuestionnaireId("questionnaire-1");

    @InjectMocks
    private ValidatedQuestionnaireResponseService subject;

    @Mock
    private ConcreteQuestionnaireResponseService questionnaireResponseService;

    @Mock
    private AccessValidator accessValidator;

    @Test
    void getQuestionnaireById() {
        fail("Test logic is expected");
    }

    @Test
    void getQuestionnaires() {
        fail("Test logic is expected");
    }

    @Test
    void createQuestionnaire() {
        fail("Test logic is expected");
    }

    @Test
    void updateQuestionnaire() {
        fail("Test logic is expected");
    }

    @Test
    void retireQuestionnaire() {
        fail("Test logic is expected");
    }

    @Test
    void getPlanDefinitionsThatIncludes() {
        fail("Test logic is expected");
    }

    @Test
    public void getQuestionnaireResponses_accessViolation_throwsException() throws Exception {
        QualifiedId.CarePlanId carePlanId = CAREPLAN_ID_1;
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);

        QuestionnaireResponseModel response = QuestionnaireResponseModel.builder().build();

        Pagination pagination = new Pagination(1, 0);

        Mockito.when(questionnaireResponseService.getQuestionnaireResponses(carePlanId, questionnaireIds, pagination)).thenReturn(List.of(response));
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(List.of(response));

        assertThrows(AccessValidationException.class, () -> subject.getQuestionnaireResponses(carePlanId, questionnaireIds, pagination));
    }
}