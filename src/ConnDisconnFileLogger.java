import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class ConnDisconnFileLogger extends FileLogger {

    public ConnDisconnFileLogger(String host, String logFilename) throws
            IOException, TimeoutException {
        super(host, logFilename);
    }

    @Override
    protected List<String> getBindings() {
        List<String> bindings = new LinkedList<>();
        bindings.add(Configuration.LogsConnectionTag);
        bindings.add(Configuration.LogsDisconnectionTag);
        return bindings;
    }

    public static void main(String[] argv) throws Exception {

        ConnDisconnFileLogger fileLogger =
                new ConnDisconnFileLogger(Configuration.RabbitMQHost,
                        argv[0]);
    }
}
