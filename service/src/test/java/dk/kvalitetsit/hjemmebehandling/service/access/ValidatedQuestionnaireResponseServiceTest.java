package dk.kvalitetsit.hjemmebehandling.service.access;

import dk.kvalitetsit.hjemmebehandling.model.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireResponseRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.implementation.ConcreteCarePlanService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Optional;

import static dk.kvalitetsit.hjemmebehandling.service.Constants.*;
import static dk.kvalitetsit.hjemmebehandling.service.MockFactory.buildQuestionnaireResponse;
import static org.junit.jupiter.api.Assertions.*;

class ValidatedQuestionnaireResponseServiceTest {


    @InjectMocks
    private ValidatedQuestionnaireService subject;

    @Mock
    private ConcreteCarePlanService carePlanService;

    @Mock
    private AccessValidator accessValidator;

    @Mock
    private QuestionnaireResponseRepository questionnaireResponseRepository;


    @Test
    void getQuestionnaireResponses() {
        fail("Test logic is expected");
    }

    @Test
    void testGetQuestionnaireResponses() {
        fail("Test logic is expected");
    }

    @Test
    void getQuestionnaireResponsesByStatus() {
        fail("Test logic is expected");
    }

    @Test
    void updateExaminationStatus() {
        fail("Test logic is expected");
    }

    @Test
    void testGetQuestionnaireResponsesByStatus() {
        fail("Test logic is expected");
    }

//  TODO: THE CODE BELOW HAS TO BE INCLUDED!
//
//    @Test
//    public void updateExaminationStatus_accessViolation_throwsException() throws Exception {
//        QualifiedId.QuestionnaireResponseId id = new QualifiedId.QuestionnaireResponseId("questionnaireresponse-1");
//        ExaminationStatus status = ExaminationStatus.UNDER_EXAMINATION;
//
//        QuestionnaireResponseModel response = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1, ORGANIZATION_ID_2);
//        Mockito.when(questionnaireResponseRepository.fetch(id)).thenReturn(Optional.of(response));
//        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(response);
//
//        assertThrows(AccessValidationException.class, () -> subject.updateExaminationStatus(id, status));
//    }

}