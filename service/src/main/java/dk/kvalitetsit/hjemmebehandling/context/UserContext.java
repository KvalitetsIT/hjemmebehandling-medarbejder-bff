package dk.kvalitetsit.hjemmebehandling.context;

public class UserContext {

	
	private String sorCode;
	private String firstName;
	private String lastName;
	private String fullName;
	private String[] autorisationsids;
	private String userid;
	private String email;
	private String[] entitlements;


    public UserContext() {

    }

    public UserContext(String sorCode) {
        this.sorCode = sorCode;
    }

    public String getSorCode() {
        return sorCode;
    }

    public void setSorCode(String sorCode) {
        this.sorCode = sorCode;
    }

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String[] getAutorisationsids() {
		return autorisationsids;
	}

	public void setAutorisationsids(String[] autorisationsids) {
		this.autorisationsids = autorisationsids;
	}

	public String getUserID() {
		return userid;
	}

	public void setUserID(String userId) {
		this.userid = userId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String[] getEntitlements() {
		return entitlements;
	}

	public void setEntitlements(String[] entitlements) {
		this.entitlements = entitlements;
	}
}
