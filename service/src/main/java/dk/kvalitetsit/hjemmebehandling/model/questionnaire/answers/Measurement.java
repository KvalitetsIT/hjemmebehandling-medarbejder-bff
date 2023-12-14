package dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers;

import java.util.Objects;

public class Measurement extends Answer<Double> {

    private double value;
    private String system;
    private String code;
    private String display;

    public Measurement(double value) {
        this.value = value;
    }

    public Measurement(double value, String system, String code, String display) {
        this.value = value;
        this.system = system;
        this.code = code;
        this.display = display;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Measurement that = (Measurement) o;
        return Double.compare(value, that.value) == 0 && Objects.equals(system, that.system) && Objects.equals(code, that.code) && Objects.equals(display, that.display);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, system, code, display);
    }

    @Override
    public dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers.Answer toDto() {
        return  new dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.answers.Measurement(this.value, this.system, this.code, this.display);
    }
}
