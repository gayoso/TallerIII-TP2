import java.util.LinkedList;
import java.util.List;

public class UserConnectResponse {

    // request
    public String username;
    public String radio;

    // id
    public String returnQueueName;

    // response
    public boolean couldConnect = false;
    public int connectionId;
    public List<UserDisconnectRequest> closedConnections = new LinkedList<>();

    public UserConnectResponse(UserConnectRequest request) {
        this.username = request.username;
        this.radio = request.radio;
        this.returnQueueName = request.returnQueueName;
    }

    public String toLogLine() {
        return Configuration.LogsConnectionTag + " " + username + " " +
                radio + " " + connectionId;
    }
}
