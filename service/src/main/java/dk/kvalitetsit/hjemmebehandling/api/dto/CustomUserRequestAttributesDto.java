package dk.kvalitetsit.hjemmebehandling.api.dto;

public class CustomUserRequestAttributesDto {

	private String cpr;
	private String initials;
	
	public String getCpr() {
		return cpr;
	}
	public void setCpr(String cpr) {
		this.cpr = cpr;
	}
	public String getInitials() {
		return initials;
	}
	public void setInitials(String initials) {
		this.initials = initials;
	}
	
}
