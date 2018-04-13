import java.util.Date;

public class UsersDBRowRadioConnection {

    public String radio;
    // para diferencias un usuario con varias conexiones a la misma radio
    public int connectionID;
    public Date keepAlive;

    public UsersDBRowRadioConnection(String radio, Date keepAlive,
                                     int connectionID){
        this.radio = radio;
        this.keepAlive = keepAlive;
        this.connectionID = connectionID;
    }
}
