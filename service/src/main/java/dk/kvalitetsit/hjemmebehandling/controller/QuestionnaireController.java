package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.QuestionnaireDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class QuestionnaireController {
    @GetMapping(value = "/v1/questionnaire")    
    public List<QuestionnaireDto> getQuestionnaires(String planDefinitionId) {
        throw new UnsupportedOperationException();
    }
}
