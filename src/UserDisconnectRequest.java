public class UserDisconnectRequest {

    // request
    public String username;
    public String radio;
    public int connectionId;

    public UserDisconnectRequest(String username, String radio, int
            connectionId) {
        this.username = username;
        this.radio = radio;
        this.connectionId = connectionId;
    }

    public String toLogLine() {
        return Configuration.LogsDisconnectionTag + " " + username + " " +
                radio + " " + connectionId;
    }
}
