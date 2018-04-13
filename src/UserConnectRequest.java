
public class UserConnectRequest {

    // request
    public String username;
    public String radio;

    // id
    public String returnQueueName;

    public UserConnectRequest(String username, String radio,
                              String returnQueueName) {
        this.username = username;
        this.radio = radio;
        this.returnQueueName = returnQueueName;
    }
}
