import java.util.LinkedList;
import java.util.List;

public class UsersDBRow extends DatabaseRow {

    public String username;
    public List<UsersDBRowRadioConnection> connections = new LinkedList<>();
    public long secondsListening;
    public int connectionsLimit;

    public UsersDBRow(String username) {
        super(username);

        this.username = username;
        this.secondsListening = 0;
        this.connectionsLimit = Configuration.MaxConnectionsPerFreeUser;
    }
}
