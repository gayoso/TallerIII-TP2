import java.util.LinkedList;
import java.util.List;

// clase que representa los usuarios que se van a persistir
public class User extends DatabaseRow {

    public String username;
    public List<UserRadioConnection> connections = new LinkedList<>();
    public long secondsListening;
    public int connectionsLimit;

    public User(String username) {
        super(username);

        this.username = username;
        this.secondsListening = 0;
        this.connectionsLimit = Configuration.MaxConnectionsPerFreeUser;
    }
}
