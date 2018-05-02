import java.util.LinkedList;
import java.util.List;

// respuesta a un pedido de conexion
public class UserConnectResponse {

    // request
    private String username;
    private String radio;

    // id
    private String returnQueueName;

    // response
    private boolean couldConnect = false;
    private int connectionId;
    private List<UserDisconnectRequest> closedConnections = new LinkedList<>();

    public UserConnectResponse(UserConnectRequest request) {
        this.setUsername(request.getUsername());
        this.setRadio(request.getRadio());
        this.setReturnQueueName(request.getReturnQueueName());
    }

    public String toLogLine() {
        return Configuration.LogsConnectionTag + " " + getUsername() + " " +
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

    public String getReturnQueueName() {
        return returnQueueName;
    }

    public void setReturnQueueName(String returnQueueName) {
        this.returnQueueName = returnQueueName;
    }

    public boolean isCouldConnect() {
        return couldConnect;
    }

    public void setCouldConnect(boolean couldConnect) {
        this.couldConnect = couldConnect;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public List<UserDisconnectRequest> getClosedConnections() {
        return closedConnections;
    }

    public void setClosedConnections(List<UserDisconnectRequest> closedConnections) {
        this.closedConnections = closedConnections;
    }
}
