package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.constants.*;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Enumeration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class FhirMapper {
    @Autowired
    private DateProvider dateProvider;

    @SafeVarargs
    static <E> List<E> listOfNullables(E... elements) {
        ArrayList<E> out = new ArrayList<E>();

        Arrays.stream(elements).forEach(e -> {
            if (e != null) {
                out.add(e);
            }
        });
        return out.stream().toList();
    }

    public Enumerations.PublicationStatus mapStatus(PlanDefinitionStatus status) {
        return switch (status) {
            case DRAFT -> Enumerations.PublicationStatus.DRAFT;
            case ACTIVE -> Enumerations.PublicationStatus.ACTIVE;
            case RETIRED -> Enumerations.PublicationStatus.RETIRED;
        };
    }

    public PatientModel mapPatient(Patient patient) {
        throw new NotImplementedException();
    }

    public CarePlanModel mapCarePlan(CarePlan carePlan) {
        throw new NotImplementedException();
    }

    public Practitioner mapPractitionerModel(PractitionerModel practitioner) {
        throw new NotImplementedException();
    }

    public PlanDefinitionModel mapPlanDefinition(PlanDefinition planDefinition) {
        throw new NotImplementedException();
    }

    public QuestionnaireResponseModel mapQuestionnaireResponse(QuestionnaireResponse questionnaireResponse) {
        throw new NotImplementedException();
    }

    public CarePlan.CarePlanStatus mapCarePlanStatus(CarePlanStatus carePlanStatus) {
        return switch (carePlanStatus) {
            case ACTIVE -> CarePlan.CarePlanStatus.ACTIVE;
            case COMPLETED -> CarePlan.CarePlanStatus.COMPLETED;
        };
    }

    public CarePlan mapCarePlanModel(CarePlanModel carePlanModel) {
        CarePlan carePlan = new CarePlan();

        mapBaseAttributesToFhir(carePlan, carePlanModel);

        carePlan.setTitle(carePlanModel.title());
        carePlan.setStatus(Enum.valueOf(CarePlan.CarePlanStatus.class, carePlanModel.status().toString()));
        carePlan.setCreated(Date.from(carePlanModel.created()));

        Optional.ofNullable(carePlanModel.startDate()).ifPresent(s -> {
            carePlan.setPeriod(new Period());
            carePlan.getPeriod().setStart(Date.from(s));
        });

        carePlan.addExtension(ExtensionMapper.mapCarePlanSatisfiedUntil(carePlanModel.satisfiedUntil()));

        // Set the subject
        Optional.ofNullable(carePlanModel.patient().id()).ifPresent(id -> carePlan.setSubject(new Reference(id.toString())));

        // Map questionnaires to activities
        Optional.ofNullable(carePlanModel.questionnaires()).ifPresent(questionnaires -> {
            carePlan.setActivity(questionnaires.stream()
                    .map(this::buildCarePlanActivity)
                    .toList());
        });


        Optional.ofNullable(carePlanModel.planDefinitions()).ifPresent(planDefinitions -> {
            // Add references to planDefinitions
            carePlan.setInstantiatesCanonical(planDefinitions
                    .stream()
                    .map(pd -> new CanonicalType(pd.id().toString()))
                    .toList());
        });

        return carePlan;
    }

    public CarePlanModel mapCarePlan(CarePlan carePlan, List<Questionnaire> questionnaires, List<PlanDefinition> planDefinitions, Organization organization, String organisationId, PatientModel patient, List<PlanDefinitionModel> plandefinitions) {
        var id = carePlan.getId();

        var wrapper = carePlan.getActivity().stream().map(activity -> {
            String questionnaireId = activity.getDetail().getInstantiatesCanonical().getFirst().getValue();
            var questionnaire = questionnaires
                    .stream()
                    .filter(x -> x.getIdElement().toUnqualifiedVersionless().getValue().equals(questionnaireId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(String.format("Could not look up Questionnaire for CarePlan %s!", id)));

            var questionnaireModel = mapQuestionnaire(questionnaire);
            var frequencyModel = mapTiming(activity.getDetail().getScheduledTiming()).orElse(null);


            // get thresholds from questionnaire
            List<ThresholdModel> thresholds = ExtensionMapper.extractThresholds(
                    questionnaire.getItem().stream()
                            .flatMap(q -> q.getExtensionsByUrl(Systems.THRESHOLD).stream())
                            .toList()
            );


            List<PlanDefinitionModel> planDefinitionModels = planDefinitions.stream()
                    .map(x -> this.mapPlanDefinition(x, questionnaire, organization))
                    .toList();

            thresholds.addAll(planDefinitionModels.stream()
                    .flatMap(p -> p.questionnaires().stream())
                    .filter(q -> q.questionnaire().id().equals(questionnaireModel.id()))
                    .findFirst()
                    .map(QuestionnaireWrapperModel::thresholds).orElse(List.of()));

            return new QuestionnaireWrapperModel(
                    questionnaireModel,
                    frequencyModel,
                    ExtensionMapper.extractActivitySatisfiedUntil(activity.getDetail().getExtension()),
                    thresholds
            );
        }).toList();

        String organizationId = ExtensionMapper.extractOrganizationId(carePlan.getExtension());


        CarePlanStatus carePlanStatus = Enum.valueOf(CarePlanStatus.class, carePlan.getStatus().toString());


        return new CarePlanModel(
                extractId(carePlan),
                organizationId,
                carePlan.getTitle(),
                carePlanStatus,
                carePlan.getCreated().toInstant(),
                carePlan.getPeriod().getStart().toInstant(),
                Optional.ofNullable(carePlan.getPeriod().getEnd()).map(Date::toInstant).orElse(null),
                patient,
                wrapper,
                plandefinitions,
                organization.getName(),
                ExtensionMapper.extractCarePlanSatisfiedUntil(carePlan.getExtension())
        );
    }

    public Timing mapFrequencyModel(FrequencyModel frequencyModel) {
        Timing timing = new Timing();

        Timing.TimingRepeatComponent repeat = new Timing.TimingRepeatComponent();

        EnumFactory<Timing.DayOfWeek> factory = new Timing.DayOfWeekEnumFactory();
        repeat.setDayOfWeek(frequencyModel.weekdays().stream().map(w -> new Enumeration<>(factory, w.toString().toLowerCase())).toList());
        repeat.setTimeOfDay(List.of(new TimeType(frequencyModel.timeOfDay().toString())));
        timing.setRepeat(repeat);

        return timing;
    }

    public Patient mapPatientModel(PatientModel patientModel) {
        Patient patient = new Patient();

        // Id may be null, in case we are creating the patient.
        if (patientModel.id() != null) {
            patient.setId(patientModel.id().toString());
        }

        var name = buildName(patientModel.name().given().getFirst(), patientModel.name().family());
        patient.addName(name);

        patient.getIdentifier().add(makeCprIdentifier(patientModel.cpr()));

        patient.addExtension(ExtensionMapper.mapCustomUserId(patientModel.customUserId()));
        patient.addExtension(ExtensionMapper.mapCustomUserName(patientModel.customUserName()));

        if (patientModel.contactDetails() != null) {
            var contactDetails = patientModel.contactDetails();

            var address = buildAddress(contactDetails);
            patient.addAddress(address);

            if (contactDetails.primaryPhone() != null) {
                var primaryContactPoint = buildContactPoint(contactDetails.primaryPhone(), 1);
                patient.addTelecom(primaryContactPoint);
            }

            if (contactDetails.secondaryPhone() != null) {
                var secondaryContactPoint = buildContactPoint(contactDetails.secondaryPhone(), 2);
                patient.addTelecom(secondaryContactPoint);
            }
        }

        if (patientModel.primaryContact().name() != null) {
            var contact = new Patient.ContactComponent();

            var contactName = buildName(patientModel.primaryContact().name());
            contact.setName(contactName);
            var organisation = new Reference();
            organisation.setReference(patientModel.primaryContact().organisation());
            contact.setOrganization(organisation);

            if (patientModel.primaryContact().affiliation() != null) {

                var codeableConcept = new CodeableConcept();
                codeableConcept.setText(patientModel.primaryContact().affiliation());
                contact.setRelationship(List.of(codeableConcept));
            }

            if (patientModel.primaryContact().contactDetails() != null) {
                var primaryRelativeContactDetails = patientModel.primaryContact().contactDetails();
                if (primaryRelativeContactDetails.primaryPhone() != null) {
                    var relativePrimaryContactPoint = buildContactPoint(primaryRelativeContactDetails.primaryPhone(), 1);
                    contact.addTelecom(relativePrimaryContactPoint);
                }

                if (primaryRelativeContactDetails.secondaryPhone() != null) {
                    var relativeSecondaryContactPoint = buildContactPoint(primaryRelativeContactDetails.secondaryPhone(), 2);
                    contact.addTelecom(relativeSecondaryContactPoint);
                }
            }

            patient.addContact(contact);
        }

        return patient;
    }

    public PatientModel mapPatient(Patient patient, @NotNull String organizationId) {

        Optional.ofNullable(organizationId).orElseThrow(() -> new IllegalStateException("Mapping contact is only possible while the organization id is known"));

        var primaryContact = Optional.ofNullable(patient.getContact()).flatMap(contacts -> {
            var optionalContact = contacts.stream()
                    .filter(c -> c.getOrganization().getReference().equals(organizationId))
                    .findFirst();

            return optionalContact.map(contact -> {
                // Extract phone numbers
                var primaryRelativeContactDetails = ContactDetailsModel.builder();

                Optional.ofNullable(contact.getTelecom()).ifPresent(telecoms -> {
                    telecoms.forEach(telecom -> {
                        if (telecom.getRank() == 1) {
                            primaryRelativeContactDetails.primaryPhone(telecom.getValue());
                        }
                        if (telecom.getRank() == 2) {
                            primaryRelativeContactDetails.secondaryPhone(telecom.getValue());
                        }
                    });
                });


                return new PrimaryContactModel(
                        primaryRelativeContactDetails.build(),
                        contact.getName().getText(),
                        contact.getRelationshipFirstRep().getText(),
                        contact.getOrganization().getReference()
                );
            });
        }).orElse(null);

        return PatientModel.builder()
                .id(extractId(patient))
                .customUserId(ExtensionMapper.extractCustomUserId(patient.getExtension()))
                .customUserName(ExtensionMapper.extractCustomUserName(patient.getExtension()))
                .name(new PersonNameModel(extractFamilyName(patient), List.of(Objects.requireNonNull(extractGivenNames(patient)))))
                .cpr(extractCpr(patient))
                .contactDetails(extractPatientContactDetails(patient))
                .primaryContact(primaryContact)
                .build();

    }

    public PlanDefinitionModel mapPlanDefinition(PlanDefinition planDefinition, Questionnaire questionnaire, Organization organization) {
        return new PlanDefinitionModel(
                extractId(planDefinition),
                ExtensionMapper.extractOrganizationId(planDefinition.getExtension()),
                planDefinition.getName(),
                planDefinition.getTitle(),
                Enum.valueOf(PlanDefinitionStatus.class, planDefinition.getStatus().toString()),
                Optional.ofNullable(planDefinition.getDate()).map(Date::toInstant).orElse(null),
                Optional.ofNullable(planDefinition.getMeta().getLastUpdated()).map(Date::toInstant).orElse(null),
                planDefinition.getAction().stream().map(a -> mapPlanDefinitionAction(a, questionnaire, organization)).toList()

        );
    }

    public Questionnaire mapQuestionnaireModel(QuestionnaireModel questionnaireModel) {
        Questionnaire questionnaire = new Questionnaire();

        mapBaseAttributesToFhir(questionnaire, questionnaireModel);

        questionnaire.setTitle(questionnaireModel.title());
        questionnaire.setStatus(mapQuestionnaireStatus(questionnaireModel.status()));
        questionnaire.getItem().addAll(questionnaireModel.questions().stream()
                .map(this::mapQuestionnaireItem)
                .toList());
        if (questionnaireModel.callToAction() != null) {
            questionnaire.getItem().add(mapQuestionnaireCallToActions(questionnaireModel.callToAction()));
        }
        questionnaire.setVersion(questionnaireModel.version());

        return questionnaire;
    }

    public QuestionnaireModel mapQuestionnaire(Questionnaire questionnaire) {

        List<QuestionModel> questions = questionnaire.getItem().stream()
                .filter(q -> !q.getLinkId().equals(Systems.CALL_TO_ACTION_LINK_ID)) // filter out call-to-action's
                .map(this::mapQuestionnaireItem).toList();

        QuestionModel callToActions = questionnaire.getItem().stream()
                .filter(q -> q.getLinkId().equals(Systems.CALL_TO_ACTION_LINK_ID)) // process call-to-action's
                .findFirst()
                .map(this::mapQuestionnaireItem)
                .orElse(null);

        return new QuestionnaireModel(
                extractId(questionnaire),
                ExtensionMapper.extractOrganizationId(questionnaire.getExtension()),
                questionnaire.getTitle(),
                null,
                mapQuestionnaireStatus(questionnaire.getStatus()),
                questions,
                callToActions,
                questionnaire.getVersion(),
                questionnaire.getMeta().getLastUpdated()
        );
    }


    public QuestionnaireResponse mapQuestionnaireResponseModel(QuestionnaireResponseModel questionnaireResponseModel) {
        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse()
                .setQuestionnaire(questionnaireResponseModel.questionnaireId().toString())
                .setBasedOn(List.of(new Reference(questionnaireResponseModel.carePlanId().toString())))
                .setAuthor(new Reference(questionnaireResponseModel.authorId().toString()))
                .setSource(new Reference(questionnaireResponseModel.sourceId().toString()))
                .setAuthored(Date.from(questionnaireResponseModel.answered()))
                .setItem(questionnaireResponseModel.questionAnswerPairs().stream().map(x -> getQuestionnaireResponseItem(x.answer())).toList())
                .setSubject(new Reference(questionnaireResponseModel.patient().id().toString()));


        mapBaseAttributesToFhir(questionnaireResponse, questionnaireResponseModel);

        List<Extension> extensions = listOfNullables(
                ExtensionMapper.mapExaminationStatus(questionnaireResponseModel.examinationStatus()),
                ExtensionMapper.mapExaminationAuthor(questionnaireResponseModel.examinationAuthor()),
                ExtensionMapper.mapTriagingCategory(questionnaireResponseModel.triagingCategory())
        );

        questionnaireResponse.setExtension(extensions);

        return questionnaireResponse;
    }

    public QuestionnaireResponseModel mapQuestionnaireResponse(QuestionnaireResponse questionnaireResponse, String organisationId, Questionnaire questionnaire, Practitioner examinationAuthor, Patient patient) {
        QuestionnaireResponseModel questionnaireResponseModel = constructQuestionnaireResponse(questionnaireResponse, organisationId, null, null, questionnaire, examinationAuthor, patient);

        // Populate questionAnswerMap
        List<QuestionAnswerPairModel> answers = questionnaireResponse.getItem().stream().map(item -> {
            QuestionModel question;
            try {
                question = getQuestion(questionnaire, item.getLinkId());
            } catch (IllegalStateException e) {
                // Corresponding question could not be found in the current/newest questionnaire
                // ignore
                // Or use the overloaded version which runs thought historical versions as well
                // and returns deprecated questions
                question = null;
            }
            AnswerModel answer = getAnswer(item);
            return new QuestionAnswerPairModel(question, answer);
        }).toList();

        return QuestionnaireResponseModel.Builder
                .from(questionnaireResponseModel).questionAnswerPairs(answers)
                .build();
    }


    public Optional<FrequencyModel> mapTiming(Timing timing) {
        return Optional.ofNullable(timing.getRepeat()).map(repeat -> new FrequencyModel(
                repeat.getDayOfWeek().stream().map(d -> Enum.valueOf(Weekday.class, d.getValue().toString())).toList(),
                !repeat.getTimeOfDay().isEmpty() ? LocalTime.parse(repeat.getTimeOfDay().getFirst().getValue()) : null
        ));
    }


    public PlanDefinition mapPlanDefinitionModel(PlanDefinitionModel planDefinitionModel) {
        PlanDefinition planDefinition = new PlanDefinition()
                .setTitle(planDefinitionModel.title())
                .setStatus(Enumerations.PublicationStatus.valueOf(planDefinitionModel.status().toString()));


        mapBaseAttributesToFhir(planDefinition, planDefinitionModel);

        // Map questionnaires to actions
        if (planDefinitionModel.questionnaires() != null) {
            planDefinition.setAction(planDefinitionModel.questionnaires()
                    .stream()
                    .map(this::buildPlanDefinitionAction)
                    .toList());
        }

        return planDefinition;
    }

    public String extractCpr(Patient patient) {
        return patient.getIdentifier().getFirst().getValue();
    }

    private void mapBaseAttributesToFhir(DomainResource target, BaseModel source) {
        // We may be creating the resource, and in that case, it is perfectly ok for it not to have id and organization id.
        Optional.ofNullable(source.id()).ifPresent(id -> target.setId(id.toString()));
        Optional.ofNullable(source.organizationId()).ifPresent(id -> target.addExtension(ExtensionMapper.mapOrganizationId(source.organizationId())));
    }

    private QualifiedId extractId(DomainResource resource) {
        String unqualifiedVersionless = resource.getIdElement().toUnqualifiedVersionless().getValue();
        if (FhirUtils.isPlainId(unqualifiedVersionless)) {
            return new QualifiedId(unqualifiedVersionless, resource.getResourceType());
        } else if (FhirUtils.isQualifiedId(unqualifiedVersionless, resource.getResourceType())) {
            return new QualifiedId(unqualifiedVersionless);
        } else {
            throw new IllegalArgumentException(String.format("Illegal id for resource of type %s: %s!", resource.getResourceType(), unqualifiedVersionless));
        }
    }

    private Identifier makeCprIdentifier(String cpr) {
        return new Identifier()
                .setSystem(Systems.CPR)
                .setValue(cpr);
    }

    private Enumerations.PublicationStatus mapQuestionnaireStatus(QuestionnaireStatus status) {
        return switch (status) {
            case ACTIVE -> Enumerations.PublicationStatus.ACTIVE;
            case DRAFT -> Enumerations.PublicationStatus.DRAFT;
            case RETIRED -> Enumerations.PublicationStatus.RETIRED;
        };
    }

    private QuestionnaireStatus mapQuestionnaireStatus(Enumerations.PublicationStatus status) {
        return switch (status) {
            case ACTIVE -> QuestionnaireStatus.ACTIVE;
            case DRAFT -> QuestionnaireStatus.DRAFT;
            case RETIRED -> QuestionnaireStatus.RETIRED;
            default ->
                    throw new IllegalArgumentException(String.format("Don't know how to map Questionnaire.status %s", status));
        };
    }

    private HumanName buildName(String givenName, String familyName) {
        return buildName(givenName, familyName, null);
    }

    private HumanName buildName(String text) {
        return buildName(null, null, text);
    }

    private HumanName buildName(String givenName, String familyName, String text) {
        return new HumanName()
                .addGiven(givenName)
                .setFamily(familyName)
                .setText(text);
    }

    private Address buildAddress(ContactDetailsModel contactDetailsModel) {
        return new Address()
                .addLine(contactDetailsModel.street())
                .setPostalCode(contactDetailsModel.postalCode())
                .setCity(contactDetailsModel.city());
    }

    private ContactPoint buildContactPoint(String phone, int rank) {
        return new ContactPoint()
                .setSystem(ContactPoint.ContactPointSystem.PHONE)
                .setValue(phone)
                .setRank(rank);
    }

    private String extractFamilyName(Patient patient) {
        if (patient.getName() == null || patient.getName().isEmpty()) {
            return null;
        }
        return patient.getName().getFirst().getFamily();
    }

    private String extractGivenNames(Patient patient) {
        if (patient.getName() == null || patient.getName().isEmpty()) {
            return null;
        }
        return patient.getName().getFirst().getGivenAsSingleString();
    }

    private ContactDetailsModel extractPatientContactDetails(Patient patient) {
        var street = Optional.ofNullable(patient.getAddressFirstRep().getLine()).map(lines -> lines.stream().map(PrimitiveType::getValue).collect(Collectors.joining(", "))).orElse(null);
        return new ContactDetailsModel(
                street,
                patient.getAddressFirstRep().getPostalCode(),
                patient.getAddressFirstRep().getCountry(),
                patient.getAddressFirstRep().getCity(),
                extractPrimaryPhone(patient.getTelecom()),
                extractSecondaryPhone(patient.getTelecom())
        );
    }


    private String extractPrimaryPhone(List<ContactPoint> contactPoints) {
        return extractPhone(contactPoints, 1);
    }

    private String extractSecondaryPhone(List<ContactPoint> contactPoints) {
        return extractPhone(contactPoints, 2);
    }

    private String extractPhone(List<ContactPoint> contactPoints, int rank) {
        if (contactPoints == null || contactPoints.isEmpty()) {
            return null;
        }
        for (ContactPoint cp : contactPoints) {
            if (cp.getSystem().equals(ContactPoint.ContactPointSystem.PHONE) && cp.getRank() == rank) {
                return cp.getValue();
            }
        }
        return null;
    }

    private QuestionModel getQuestion(QuestionnaireModel questionnaire, String linkId) {
        return getQuestion(mapQuestionnaireModel(questionnaire), linkId);
    }


    private QuestionModel getQuestion(Questionnaire questionnaire, String linkId) {
        return Optional.ofNullable(getQuestionnaireItem(questionnaire, linkId))
                .map(this::mapQuestionnaireItem)
                .orElseThrow(() -> new IllegalStateException(String.format("Malformed QuestionnaireResponse: Question for linkId %s not found in Questionnaire %s!", linkId, questionnaire.getId())));
    }

    private Questionnaire.QuestionnaireItemComponent getQuestionnaireItem(Questionnaire questionnaire, String linkId) {
        return questionnaire.getItem().stream()
                .filter(Objects::nonNull)
                .filter(item -> linkId.equals(item.getLinkId()))
                .findFirst()
                .orElse(null);
    }

    private Questionnaire.QuestionnaireItemComponent mapQuestionnaireItem(QuestionModel question) {
        Questionnaire.QuestionnaireItemComponent item = new Questionnaire.QuestionnaireItemComponent();

        item.setLinkId(question.linkId());
        item.setText(question.text());
        if (question.abbreviation() != null) {
            item.addExtension(ExtensionMapper.mapQuestionAbbreviation(question.abbreviation()));
        }
        if (question.thresholds() != null) {
            item.getExtension().addAll(ExtensionMapper.mapThresholds(question.thresholds()));
        }
        if (question.helperText() != null) {
            item.addItem(mapQuestionHelperText(question.helperText()));
        }
        item.setRequired(question.required());
        if (question.options() != null) {
            item.setAnswerOption(mapAnswerOptions(question.options()));
        }
        item.setType(mapQuestionType(question.questionType()));
        if (question.enableWhens() != null) {
            item.setEnableWhen(mapEnableWhens(question.enableWhens()));
        }

        if (question.measurementType() != null) {
            item.getCodeFirstRep()
                    .setCode(question.measurementType().code())
                    .setDisplay(question.measurementType().display())
                    .setSystem(question.measurementType().system());
        }

        if (item.getType() == Questionnaire.QuestionnaireItemType.GROUP) {
            question.subQuestions().forEach(questionModel -> {
                item.addItem(this.mapQuestionnaireItem(questionModel));
            });
        }
        return item;
    }

    private QuestionModel mapQuestionnaireItem(Questionnaire.QuestionnaireItemComponent item) {
        return new QuestionModel(
                item.getLinkId(),
                item.getText(),
                ExtensionMapper.extractQuestionAbbreviation(item.getExtension()),
                mapQuestionnaireItemHelperText(item.getItem()),
                item.getRequired(),
                mapQuestionType(item.getType()),
                item.hasCode() ? mapCodingConcept(item.getCodeFirstRep().getSystem(), item.getCodeFirstRep().getCode(), item.getCodeFirstRep().getDisplay()) : null,
                Optional.ofNullable(item.getAnswerOption()).map(this::mapAnswerOptionComponents).orElse(null),
                item.hasEnableWhen() ? mapEnableWhenComponents(item.getEnableWhen()) : null,
                ExtensionMapper.extractThresholds(item.getExtensionsByUrl(Systems.THRESHOLD)),
                item.getType() == Questionnaire.QuestionnaireItemType.GROUP ? mapQuestionnaireItemGroupQuestions(item.getItem()) : null,
                false
        );
    }

    private List<QuestionModel> mapQuestionnaireItemGroupQuestions(List<Questionnaire.QuestionnaireItemComponent> item) {
        return item.stream()
                .filter(i -> i.getType() != Questionnaire.QuestionnaireItemType.DISPLAY)
                .map(this::mapQuestionnaireItem)
                .toList();
    }

    private String mapQuestionnaireItemHelperText(List<Questionnaire.QuestionnaireItemComponent> item) {
        return item.stream()
                .filter(i -> i.getType().equals(Questionnaire.QuestionnaireItemType.DISPLAY))
                .map(Questionnaire.QuestionnaireItemComponent::getText)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

    }

    private Questionnaire.QuestionnaireItemComponent mapQuestionHelperText(String text) {
        return new Questionnaire.QuestionnaireItemComponent()
                .setLinkId(IdType.newRandomUuid().getValueAsString())
                .setType(Questionnaire.QuestionnaireItemType.DISPLAY)
                .setText(text);
    }

    private Questionnaire.QuestionnaireItemComponent mapQuestionnaireCallToActions(QuestionModel callToAction) {
        return mapQuestionnaireItem(callToAction)
                .setType(Questionnaire.QuestionnaireItemType.DISPLAY)
                .setLinkId(Systems.CALL_TO_ACTION_LINK_ID);
    }

    private Questionnaire.QuestionnaireItemComponent mapQuestionnaireCallToAction(QuestionModel question) {
        Questionnaire.QuestionnaireItemComponent item = new Questionnaire.QuestionnaireItemComponent()
                .setLinkId(question.linkId())
                .setText(question.text())
                .setRequired(question.required())
                .setType(mapQuestionType(question.questionType()));

//        if (question.getOptions() != null) {
//             TODO: Fix the line below - it has to include comment and triage
//            item.setAnswerOption( mapAnswerOptions(question.getOptions().stream().map(Option::getOption).toList()) );
//        }

        Optional.ofNullable(question.enableWhens()).ifPresent(x -> item.setEnableWhen(mapEnableWhens(x)));

        return item;
    }

    private List<Questionnaire.QuestionnaireItemEnableWhenComponent> mapEnableWhens(List<QuestionModel.EnableWhen> enableWhens) {
        return enableWhens
                .stream()
                .map(this::mapEnableWhen)
                .toList();
    }

    private Questionnaire.QuestionnaireItemEnableWhenComponent mapEnableWhen(QuestionModel.EnableWhen enableWhen) {
        return new Questionnaire.QuestionnaireItemEnableWhenComponent()
                .setOperator(mapEnableWhenOperator(enableWhen.operator()))
                .setQuestion(enableWhen.answer().linkId())
                .setAnswer(getValue(enableWhen.answer()))
                .setOperator(mapEnableWhenOperator(enableWhen.operator()));
    }

    private List<QuestionModel.EnableWhen> mapEnableWhenComponents(List<Questionnaire.QuestionnaireItemEnableWhenComponent> enableWhen) {
        return enableWhen
                .stream()
                .map(this::mapEnableWhenComponent)
                .toList();
    }

    private QuestionModel.EnableWhen mapEnableWhenComponent(Questionnaire.QuestionnaireItemEnableWhenComponent enableWhen) {
        return new QuestionModel.EnableWhen(
                mapAnswer(enableWhen.getQuestion(), enableWhen.getAnswer()),
                mapEnableWhenOperator(enableWhen.getOperator())
        );
    }

    private AnswerModel mapAnswer(String question, Type answer) {
        return switch (answer) {
            case StringType stringType ->
                    new AnswerModel(question, stringType.asStringValue(), AnswerType.STRING, null);
            case BooleanType booleanType ->
                    new AnswerModel(question, booleanType.asStringValue(), AnswerType.BOOLEAN, null);
            case Quantity quantity ->
                    new AnswerModel(question, quantity.getValueElement().asStringValue(), AnswerType.QUANTITY, null);
            case IntegerType integerType ->
                    new AnswerModel(question, integerType.asStringValue(), AnswerType.INTEGER, null);
            case null, default ->
                    throw new IllegalArgumentException(String.format("Unsupported AnswerItem of type: %s", answer));
        };
    }

    private Questionnaire.QuestionnaireItemOperator mapEnableWhenOperator(EnableWhenOperator operator) {
        return switch (operator) {
            case EQUAL -> Questionnaire.QuestionnaireItemOperator.EQUAL;
            case LESS_THAN -> Questionnaire.QuestionnaireItemOperator.LESS_THAN;
            case LESS_OR_EQUAL -> Questionnaire.QuestionnaireItemOperator.LESS_OR_EQUAL;
            case GREATER_THAN -> Questionnaire.QuestionnaireItemOperator.GREATER_THAN;
            case GREATER_OR_EQUAL -> Questionnaire.QuestionnaireItemOperator.GREATER_OR_EQUAL;
        };
    }

    private EnableWhenOperator mapEnableWhenOperator(Questionnaire.QuestionnaireItemOperator operator) {
        return switch (operator) {
            case EQUAL -> EnableWhenOperator.EQUAL;
            case LESS_THAN -> EnableWhenOperator.LESS_THAN;
            case LESS_OR_EQUAL -> EnableWhenOperator.LESS_OR_EQUAL;
            case GREATER_THAN -> EnableWhenOperator.GREATER_THAN;
            case GREATER_OR_EQUAL -> EnableWhenOperator.GREATER_OR_EQUAL;
            default ->
                    throw new IllegalArgumentException(String.format("Don't know how to map QuestionnaireItemOperator %s", operator));
        };
    }

    private List<Questionnaire.QuestionnaireItemAnswerOptionComponent> mapAnswerOptions(List<Option> answerOptions) {
        return answerOptions
                .stream()
                .map(oc -> (Questionnaire.QuestionnaireItemAnswerOptionComponent) new Questionnaire.QuestionnaireItemAnswerOptionComponent()
                        .setValue(new StringType(oc.option()))
                        .addExtension(ExtensionMapper.mapAnswerOptionComment(oc.comment())))
                .toList();
    }

    private List<Option> mapAnswerOptionComponents(List<Questionnaire.QuestionnaireItemAnswerOptionComponent> optionComponents) {
        return optionComponents
                .stream()
                .map(oc -> new Option(oc.getValue().primitiveValue(), ExtensionMapper.extractAnswerOptionComment(oc.getExtension())))
                .toList();
    }

    private QuestionType mapQuestionType(Questionnaire.QuestionnaireItemType type) {
        return switch (type) {
            case CHOICE -> QuestionType.CHOICE;
            case INTEGER -> QuestionType.INTEGER;
            case QUANTITY -> QuestionType.QUANTITY;
            case STRING -> QuestionType.STRING;
            case BOOLEAN -> QuestionType.BOOLEAN;
            case DISPLAY -> QuestionType.DISPLAY;
            case GROUP -> QuestionType.GROUP;
            default ->
                    throw new IllegalArgumentException(String.format("Don't know how to map QuestionnaireItemType %s", type));
        };
    }

    private Questionnaire.QuestionnaireItemType mapQuestionType(QuestionType type) {
        return switch (type) {
            case CHOICE -> Questionnaire.QuestionnaireItemType.CHOICE;
            case INTEGER -> Questionnaire.QuestionnaireItemType.INTEGER;
            case QUANTITY -> Questionnaire.QuestionnaireItemType.QUANTITY;
            case STRING -> Questionnaire.QuestionnaireItemType.STRING;
            case BOOLEAN -> Questionnaire.QuestionnaireItemType.BOOLEAN;
            case DISPLAY -> Questionnaire.QuestionnaireItemType.DISPLAY;
            case GROUP -> Questionnaire.QuestionnaireItemType.GROUP;
        };
    }

    private AnswerModel getAnswer(QuestionnaireResponse.QuestionnaireResponseItemComponent item) {
        String linkId = item.getLinkId();
        String value = null;
        AnswerType answerType;
        List<AnswerModel> subAnswers = List.of();

        boolean emptyAnswer = item.getAnswer().isEmpty();
        boolean hasSubAnswers = !item.getItem().isEmpty();

        if (emptyAnswer && hasSubAnswers) {
            // Group answer with sub-answers
            answerType = AnswerType.GROUP;
            subAnswers = item.getItem().stream().map(this::getAnswer).toList();
        } else {
            var answerItem = extractAnswerItem(item);

            if (answerItem.hasValueStringType()) {
                value = answerItem.getValue().primitiveValue();
            } else if (answerItem.hasValueIntegerType()) {
                value = String.valueOf(answerItem.getValueIntegerType().primitiveValue());
            } else if (answerItem.hasValueQuantity()) {
                value = answerItem.getValueQuantity().getValueElement().primitiveValue();
            } else if (answerItem.hasValueBooleanType()) {
                value = String.valueOf(answerItem.getValueBooleanType().primitiveValue());
            }

            answerType = getAnswerType(answerItem);
        }

        return new AnswerModel(linkId, value, answerType, subAnswers);
    }

    private QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent extractAnswerItem(QuestionnaireResponse.QuestionnaireResponseItemComponent item) {
        if (item.getAnswer() == null || item.getAnswer().size() != 1) {
            throw new IllegalStateException("Expected exactly one answer!");
        }
        return item.getAnswer().getFirst();
    }

    private AnswerType getAnswerType(QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent answerItem) {
        Type answerType = answerItem.getValue();
        return getAnswerType(answerType);
    }

    private AnswerType getAnswerType(Type answerType) {
        return switch (answerType) {
            case StringType stringType -> AnswerType.STRING;
            case BooleanType booleanType -> AnswerType.BOOLEAN;
            case Quantity quantity -> AnswerType.QUANTITY;
            case IntegerType integerType -> AnswerType.INTEGER;
            case null, default ->
                    throw new IllegalArgumentException(String.format("Unsupported AnswerItem of type: %s", answerType));
        };
    }

    private CarePlan.CarePlanActivityComponent buildCarePlanActivity(QuestionnaireWrapperModel questionnaireWrapperModel) {
        CanonicalType instantiatesCanonical = new CanonicalType(questionnaireWrapperModel.questionnaire().id().toString());
        Type timing = mapFrequencyModel(questionnaireWrapperModel.frequency());
        Extension activitySatisfiedUntil = ExtensionMapper.mapActivitySatisfiedUntil(questionnaireWrapperModel.satisfiedUntil());
        return buildActivity(instantiatesCanonical, timing, activitySatisfiedUntil);
    }

    private CarePlan.CarePlanActivityComponent buildActivity(CanonicalType instantiatesCanonical, Type timing, Extension activitySatisfiedUntil) {
        CarePlan.CarePlanActivityComponent activity = new CarePlan.CarePlanActivityComponent();
        activity.setDetail(buildDetail(instantiatesCanonical, timing, activitySatisfiedUntil));
        return activity;
    }

    private CarePlan.CarePlanActivityDetailComponent buildDetail(CanonicalType instantiatesCanonical, Type timing, Extension activitySatisfiedUntil) {
        CarePlan.CarePlanActivityDetailComponent detail = new CarePlan.CarePlanActivityDetailComponent()
                .setInstantiatesCanonical(List.of(instantiatesCanonical))
                .setStatus(CarePlan.CarePlanActivityStatus.NOTSTARTED)
                .setScheduled(timing);

        detail.addExtension(activitySatisfiedUntil);

        return detail;
    }

    private QuestionnaireResponse.QuestionnaireResponseItemComponent getQuestionnaireResponseItem(AnswerModel answer) {
        var item = new QuestionnaireResponse.QuestionnaireResponseItemComponent()
                .setLinkId(answer.linkId());

        item.getAnswer().add(getAnswerItem(answer));

        if (answer.answerType() == AnswerType.GROUP && answer.subAnswers() != null) {
            item.setItem(answer.subAnswers().stream().map(this::getQuestionnaireResponseItem).toList());
        }
        return item;
    }

    private QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent getAnswerItem(AnswerModel answer) {
        return new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
                .setValue(getValue(answer));
    }

    private Type getValue(AnswerModel answer) {
        if (answer == null || answer.value() == null) {
            return null;
        }
        return switch (answer.answerType()) {
            case INTEGER -> new IntegerType(answer.value());
            case STRING -> new StringType(answer.value());
            case QUANTITY -> new Quantity(Double.parseDouble(answer.value()));
            case BOOLEAN -> new BooleanType(answer.value());
            case GROUP -> null; // GROUP type doesn't have a value
        };
    }

    private QuestionnaireWrapperModel mapPlanDefinitionAction(PlanDefinition.PlanDefinitionActionComponent action, Questionnaire questionnaire, Organization organization) {
        var thresholds = ExtensionMapper.extractThresholds(action.getExtensionsByUrl(Systems.THRESHOLD));
        FrequencyModel timeType = Optional.ofNullable(ExtensionMapper.extractOrganizationDeadlineTimeDefault(organization.getExtension()))
                .map(time -> {
                    Timing timing = new Timing();
                    timing.getRepeat().getTimeOfDay().add(time);
                    return timing;
                })
                .flatMap(this::mapTiming)
                .orElse(null);

        return new QuestionnaireWrapperModel(
                mapQuestionnaire(questionnaire),
                timeType,
                null,
                thresholds
        );
    }

    public PractitionerModel mapPractitioner(Practitioner practitioner) {
        return new PractitionerModel(
                extractId(practitioner),
                practitioner.getNameFirstRep().getGivenAsSingleString(),
                practitioner.getNameFirstRep().getFamily()
        );
    }

    private MeasurementTypeModel mapCodingConcept(String system, String code, String display) {
        return new MeasurementTypeModel(system, code, display);
    }


    private PlanDefinition.PlanDefinitionActionComponent buildPlanDefinitionAction(QuestionnaireWrapperModel questionnaireWrapperModel) {
        CanonicalType definitionCanonical = new CanonicalType(questionnaireWrapperModel.questionnaire().id().toString());

        List<Extension> thresholds = ExtensionMapper.mapThresholds(questionnaireWrapperModel.thresholds());

        PlanDefinition.PlanDefinitionActionComponent action = new PlanDefinition.PlanDefinitionActionComponent();
        action.setDefinition(definitionCanonical);
        action.setExtension(thresholds);

        return action;
    }


    private QuestionnaireResponseModel constructQuestionnaireResponse(QuestionnaireResponse questionnaireResponse, String organisationId, PlanDefinition planDefinition, CarePlan carePlan, Questionnaire questionnaire, Practitioner examinationAuthor, Patient patient) {
        String qId = questionnaireResponse.getQuestionnaire();

        String patientId = questionnaireResponse.getSubject().getReference();
        String carePlanId = questionnaireResponse.getBasedOnFirstRep().getReference();

        String planDefinitionTitle = Optional.ofNullable(carePlan)
                .map(CarePlan::getInstantiatesCanonical)
                .flatMap(canonicals -> canonicals.stream()
                        .map(CanonicalType::getValue)
                        .map(planDefinitionId -> Optional.ofNullable(planDefinition)
                                .filter(p -> p.getAction().stream()
                                        .anyMatch(action -> action.getDefinitionCanonicalType().equals(qId)))
                                .map(PlanDefinition::getTitle))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst()
                )
                .orElseThrow(() -> new IllegalStateException(String.format("No matching PlanDefinition with title found for CarePlan id %s!", carePlanId)));

        var id = extractId(questionnaireResponse);
        var organizationId = ExtensionMapper.extractOrganizationId(questionnaireResponse.getExtension());
        var questionnaireId = extractId(questionnaire);

        var authorId = Optional.ofNullable(questionnaireResponse.getAuthor())
                .map(Reference::getReference)
                .map(QualifiedId::new)
                .orElseThrow(() -> new IllegalStateException(String.format("Error mapping QuestionnaireResponse %s: No Author-attribute present!!", id)));

        var sourceId = Optional.ofNullable(questionnaireResponse.getSource())
                .map(Reference::getReference)
                .map(QualifiedId::new)
                .orElseThrow(() -> new IllegalStateException(String.format("Error mapping QuestionnaireResponse %s: No Source-attribute present!!", id)));


        return new QuestionnaireResponseModel(
                id,
                organizationId,
                questionnaireId,
                new QualifiedId(carePlanId),
                authorId,
                sourceId,
                questionnaire.getName(),
                null,
                questionnaireResponse.getAuthored().toInstant(),
                ExtensionMapper.extractExaminationStatus(questionnaireResponse.getExtension()),
                mapPractitioner(examinationAuthor),
                ExtensionMapper.extractTriagingCategory(questionnaireResponse.getExtension()),
                mapPatient(patient, organisationId),
                planDefinitionTitle
        );


    }

}
