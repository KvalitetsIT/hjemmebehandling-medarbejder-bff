package dk.kvalitetsit.hjemmebehandling.context;

public class UserContext {
    private String sorCode;

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
}
