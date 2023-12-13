package dk.kvalitetsit.hjemmebehandling.model.questionnaire.answers;

import java.util.Objects;

public class Number extends Answer{

    private final double value;


    public Number(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Number number = (Number) o;
        return Double.compare(value, number.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
