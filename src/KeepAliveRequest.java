public class KeepAliveRequest {

    public String username;
    public int connectionId;
    public String radio;

    public KeepAliveRequest(String username, int connectionId, String radio) {
        this.username = username;
        this.connectionId = connectionId;
        this.radio = radio;
    }
}
