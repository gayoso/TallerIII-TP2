import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class UsersSecondsListenedStatistics {

    public List<UserSecondsListened> usersMostListenedSeconds;

    public UsersSecondsListenedStatistics(Collection<UserSecondsListened> stats) {
        this.usersMostListenedSeconds = new LinkedList<>(stats);
    }
}
