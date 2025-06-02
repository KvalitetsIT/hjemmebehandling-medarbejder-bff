package dk.kvalitetsit.hjemmebehandling.repository.access;

import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.repository.adaptation.QuestionnaireRepositoryAdaptor;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;


import static dk.kvalitetsit.hjemmebehandling.Constants.QUESTIONNAIRE_ID_1;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidatedQuestionnaireServiceTest {

    @InjectMocks
    private ValidatedQuestionnaireRepository subject;

    @Mock
    private QuestionnaireRepositoryAdaptor questionnaireRepository;

    @Mock
    private AccessValidator<QuestionnaireModel> accessValidator;

    @Test
    public void updateQuestionnaire_accessViolation_throwsException() throws Exception {
        QuestionnaireModel questionnaire = QuestionnaireModel.builder().build();
        Mockito.when(questionnaireRepository.fetch(QUESTIONNAIRE_ID_1)).thenReturn(Optional.of(questionnaire));
        Mockito.doThrow(AccessValidationException.class).when(accessValidator).validateAccess(questionnaire);
        assertThrows(AccessValidationException.class, () -> subject.fetch(QUESTIONNAIRE_ID_1));
    }

}
