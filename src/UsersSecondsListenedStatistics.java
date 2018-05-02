import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

// clase externa para el paso de estadisticas de segundos escuchados por cada
// usuario
public class UsersSecondsListenedStatistics {

    private List<UserSecondsListened> usersMostListenedSeconds;

    public UsersSecondsListenedStatistics(Collection<UserSecondsListened> stats) {
        this.setUsersMostListenedSeconds(new LinkedList<>(stats));
    }

    public List<UserSecondsListened> getUsersMostListenedSeconds() {
        return usersMostListenedSeconds;
    }

    public void setUsersMostListenedSeconds(List<UserSecondsListened> usersMostListenedSeconds) {
        this.usersMostListenedSeconds = usersMostListenedSeconds;
    }
}
