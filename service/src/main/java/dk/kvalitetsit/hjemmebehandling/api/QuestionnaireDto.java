package dk.kvalitetsit.hjemmebehandling.api;

import java.util.List;

public class QuestionnaireDto {
    private String id;
    private String title;
    private String status;
    private List<QuestionDto> questions;

    // TODO: Think about thresholds and their relation to questions ...
}
