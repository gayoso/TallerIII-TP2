
// clase que representa la entidad radio que se persiste
public class Radio extends DatabaseRow {
    private String name;
    private int connectedUsers;

    public Radio(String name) {
        super(name);

        this.setName(name);
        this.setConnectedUsers(0);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getConnectedUsers() {
        return connectedUsers;
    }

    public void setConnectedUsers(int connectedUsers) {
        this.connectedUsers = connectedUsers;
    }
}
