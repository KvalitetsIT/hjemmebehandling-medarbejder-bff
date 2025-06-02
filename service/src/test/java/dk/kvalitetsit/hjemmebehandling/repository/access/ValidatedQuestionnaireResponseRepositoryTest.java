package dk.kvalitetsit.hjemmebehandling.repository.access;

import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;
import dk.kvalitetsit.hjemmebehandling.repository.QuestionnaireResponseRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static dk.kvalitetsit.hjemmebehandling.MockFactory.buildQuestionnaireResponse;
import static dk.kvalitetsit.hjemmebehandling.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ValidatedQuestionnaireResponseRepositoryTest {


    @InjectMocks
    private ValidatedQuestionnaireResponseRepository subject;

    @Mock
    private QuestionnaireResponseRepository<QuestionnaireResponseModel> questionnaireResponseRepository;


    @Mock
    private AccessValidator<QuestionnaireResponseModel> accessValidator;


    @Test
    public void getQuestionnaireResponses_accessViolation_throwsException() throws Exception {
        List<QualifiedId.QuestionnaireId> questionnaireIds = List.of(QUESTIONNAIRE_ID_1);
        QuestionnaireResponseModel response = QuestionnaireResponseModel.builder().build();
        Mockito.when(questionnaireResponseRepository.fetch(CAREPLAN_ID_1, questionnaireIds)).thenReturn(List.of(response));
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(List.of(response));
        assertThrows(AccessValidationException.class, () -> subject.fetch(CAREPLAN_ID_1, questionnaireIds));
    }

    @Test
    public void updateExaminationStatus_accessViolation_throwsException() throws Exception {
        QuestionnaireResponseModel response = buildQuestionnaireResponse(QUESTIONNAIRE_RESPONSE_ID_1, QUESTIONNAIRE_ID_1, ORGANIZATION_ID_2);
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(response);
        assertThrows(AccessValidationException.class, () -> subject.update(response));
    }
}