package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.constants.AnswerType;

import java.util.List;

public record AnswerModel(
        String linkId,
        String value,
        AnswerType answerType,
        List<AnswerModel> subAnswers

) { }
