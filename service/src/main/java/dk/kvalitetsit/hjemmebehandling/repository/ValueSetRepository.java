package dk.kvalitetsit.hjemmebehandling.repository;

import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import org.hl7.fhir.r4.model.ValueSet;


public interface ValueSetRepository<ValueSet> extends Repository<ValueSet, QualifiedId.ValueSetId> { }
