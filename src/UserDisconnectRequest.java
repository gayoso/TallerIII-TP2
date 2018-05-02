// pedido de desconexion
public class UserDisconnectRequest {

    // request
    private String username;
    private String radio;
    private int connectionId;

    public UserDisconnectRequest(String username, String radio, int
            connectionId) {
        this.setUsername(username);
        this.setRadio(radio);
        this.setConnectionId(connectionId);
    }

    public String toLogLine() {
        return Configuration.LogsDisconnectionTag + " " + getUsername() + " " +
                getRadio() + " " + getConnectionId();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRadio() {
        return radio;
    }

    public void setRadio(String radio) {
        this.radio = radio;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }
}
