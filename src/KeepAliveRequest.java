
// clase que representa un mensaje de keep alive
public class KeepAliveRequest {

    private String username;
    private int connectionId;
    private String radio;

    public KeepAliveRequest(String username, int connectionId, String radio) {
        this.setUsername(username);
        this.setConnectionId(connectionId);
        this.setRadio(radio);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public String getRadio() {
        return radio;
    }

    public void setRadio(String radio) {
        this.radio = radio;
    }
}
