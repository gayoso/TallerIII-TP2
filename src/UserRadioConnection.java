import java.util.Date;

// clase que representa la conexion entre un usuario y una radio, para
// persistir junto con los usuarios
public class UserRadioConnection {

    public String radio;
    // para diferencias un usuario con varias conexiones a la misma radio
    public int connectionID;
    public Date keepAlive;

    public UserRadioConnection(String radio, Date keepAlive,
                               int connectionID){
        this.radio = radio;
        this.keepAlive = keepAlive;
        this.connectionID = connectionID;
    }
}
