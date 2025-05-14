package dk.kvalitetsit.hjemmebehandling.client;

import dk.kvalitetsit.hjemmebehandling.model.CPR;

public class CustomUserRequestAttributesDto {

    private CPR cpr;
    private String initials;

    public CPR getCpr() {
        return cpr;
    }

    public void setCpr(CPR cpr) {
        this.cpr = cpr;
    }

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }

}
