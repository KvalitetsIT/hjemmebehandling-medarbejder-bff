package dk.kvalitetsit.hjemmebehandling.controller;

import dk.kvalitetsit.hjemmebehandling.api.QuestionnaireResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class QuestionnaireResponseController {
    @PostMapping(value = "/v1/questionnaireresponse")
    public void createQuestionnaireResponse(QuestionnaireResponseDto questionnaireResponse) {
        throw new UnsupportedOperationException();
    }

    @GetMapping(value = "/v1/questionnaireresponse")
    public List<QuestionnaireResponseDto> getQuestionnaireResponses(String cpr) {
        throw new UnsupportedOperationException();
    }
}
