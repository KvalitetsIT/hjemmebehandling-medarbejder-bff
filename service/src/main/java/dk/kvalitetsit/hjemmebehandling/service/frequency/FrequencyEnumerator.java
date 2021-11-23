package dk.kvalitetsit.hjemmebehandling.service.frequency;

import dk.kvalitetsit.hjemmebehandling.model.FrequencyModel;

import java.time.Instant;

public class FrequencyEnumerator {
    private Instant currentPointInTime;
    private FrequencyModel frequency;

    public FrequencyEnumerator(Instant seed, FrequencyModel frequency) {
        currentPointInTime = seed;
        frequency = frequency;
    }

    public Instant getPointInTime() {
        return currentPointInTime;
    }

    public FrequencyEnumerator next() {
        return this;
    }
}
