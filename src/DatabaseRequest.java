// clase que detalla la estructura de un request a una base de datos del sistema
public class DatabaseRequest {

    private int type;
    private String serializedRequest;
    private String routingKey;

    public DatabaseRequest(int type, String serializedRequest, String
            username) {
        this.type = type;
        this.serializedRequest = serializedRequest;
        this.setRoutingKey(username.substring(0, 1));
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getSerializedRequest() {
        return serializedRequest;
    }

    public void setSerializedRequest(String serializedRequest) {
        this.serializedRequest = serializedRequest;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }
}
