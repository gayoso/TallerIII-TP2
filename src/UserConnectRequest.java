
// pedido de conexion
public class UserConnectRequest {

    // request
    private String username;
    private String radio;

    // id
    private String returnQueueName;

    public UserConnectRequest(String username, String radio,
                              String returnQueueName) {
        this.setUsername(username);
        this.setRadio(radio);
        this.setReturnQueueName(returnQueueName);
    }

    public UserConnectRequest(UserConnectResponse resp) {
        this.username = resp.getUsername();
        this.radio = resp.getRadio();
        this.returnQueueName = resp.getReturnQueueName();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRadio() {
        return radio;
    }

    public void setRadio(String radio) {
        this.radio = radio;
    }

    public String getReturnQueueName() {
        return returnQueueName;
    }

    public void setReturnQueueName(String returnQueueName) {
        this.returnQueueName = returnQueueName;
    }
}
