package dk.kvalitetsit.hjemmebehandling.model;

public class PrimaryContactModel {

    private ContactDetailsModel contactDetails;

    private String name;
    /**
     * How the primary contact relates to the patient. It may be mother, brother, friend etc.
     */
    private String affiliation;

    /**
     * The id of the organisation which this primary contacts is associated
     */
    private String organisation;

    @Override
    public String toString() {
        return "PrimaryContactMdeol{" +
                "contactDetails=" + contactDetails +
                ", name='" + name + '\'' +
                ", affiliation='" + affiliation + '\'' +
                ", organisation='" + organisation + '\'' +
                '}';
    }

    public String getOrganisation() {
        return organisation;
    }

    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ContactDetailsModel getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(ContactDetailsModel contactDetails) {
        this.contactDetails = contactDetails;
    }
}
