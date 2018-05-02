import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

// clase base para un proceso que utiliza rabbitmq
public abstract class RabbitMQProcess {

    private Connection connection;
    private Channel channel;

    public RabbitMQProcess(String host) throws IOException, TimeoutException {

        // load configuration
        Configuration.loadConfiguration("config");

        // init RabbitMQ connection and channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        connection = factory.newConnection();
        channel = connection.createChannel();

        Logger.init();

        addShutdownHook();
    }

    protected Connection getConnection() {
        return connection;
    }

    protected Channel getChannel() {
        return channel;
    }

    public void addShutdownHook() {

        RabbitMQProcess instance = this;
        Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {

                System.out.println("Calling shutdown hook");

                try {
                    instance.close();
                } catch (IOException e) {
                    Logger.output("IOEXception during shutdown hook close");
                } catch (TimeoutException e) {
                    Logger.output("TimeoutException during shutdown hook " +
                            "close");
                }
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    Logger.output("InterruptedException during shutdown hook" +
                            " close");
                }
            }
        });
    }

    protected void close() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }

    public abstract void run() throws IOException;
}
