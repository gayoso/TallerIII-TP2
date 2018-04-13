import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQProcess {

    protected Connection connection;
    protected Channel channel;

    public RabbitMQProcess(String host) throws IOException, TimeoutException {

        // load configuration
        Configuration.loadConfiguration("config");

        // init RabbitMQ connection and channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        connection = factory.newConnection();
        channel = connection.createChannel();

        addShutdownHook();
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
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    protected void close() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }

}
