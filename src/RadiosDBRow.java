import java.util.LinkedList;
import java.util.List;

public class RadiosDBRow extends DatabaseRow {
    public String name;
    public int connectedUsers;

    public RadiosDBRow(String name) {
        super(name);

        this.name = name;
        this.connectedUsers = 0;
    }
}
