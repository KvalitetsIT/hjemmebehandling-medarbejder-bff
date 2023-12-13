package dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers.Answer;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.Question;
import dk.kvalitetsit.hjemmebehandling.model.questionnaire.question.BaseQuestion;

import java.util.stream.Collectors;

public class QuestionDto<T extends Answer> extends BaseQuestionDto<T> {

    private T answer;

    public QuestionDto(String text) {
        super(text);
    }

    @Override
    public void answer(T answer) {
        this.answer = answer;
    }

    public T getAnswer() {
        return answer;
    }

    @Override
    public BaseQuestion<?> toModel() {
        Question<dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers.Answer> questionModel = new Question<>(this.getText());

        questionModel.setLinkId(this.getLinkId());
        questionModel.setAbbreviation(this.getAbbreviation());

        questionModel.setEnableWhens(this.getEnableWhens()
                .stream()
                .map(EnableWhen::toDto)
                .collect(Collectors.toList())
        );
        questionModel.answer(this.getAnswer().toModel());

        return questionModel;
    }
}
