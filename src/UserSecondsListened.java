
// clase interna para el paso de estadisticas de segundos escuchados por cada
// usuario
public class UserSecondsListened {

    private String username;
    private long secondsListened;

    public UserSecondsListened(String username, long secondsListened) {
        this.setUsername(username);
        this.setSecondsListened(secondsListened);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getSecondsListened() {
        return secondsListened;
    }

    public void setSecondsListened(long secondsListened) {
        this.secondsListened = secondsListened;
    }
}
