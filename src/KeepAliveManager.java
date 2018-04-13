import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class KeepAliveManager extends RabbitMQProcess {

    public KeepAliveManager(String host) throws IOException, TimeoutException {
        super(host);

        // declare USERS_DB exchange
        channel.exchangeDeclare(Configuration.UsersDBExchange,
                BuiltinExchangeType.DIRECT);

        consumeKeepAlives();
    }

    private String consumeKeepAlives() throws IOException {
        // KEEP ALIVE consumer
        channel.queueDeclare(Configuration.KeepAliveQueue, true, false, false,
                null);
        Consumer consumer_keepalive = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                String json = new String(body, "UTF-8");
                KeepAliveRequest request = new Gson().fromJson(json,
                        KeepAliveRequest.class);

                System.out.println(" [x] Received keep alive request from: "
                        + request.username + " to: " + request.radio + " id: " +
                        request.connectionId);

                // ask usersDB to register connection
                channel.basicPublish(Configuration.UsersDBExchange,
                        Configuration.UsersDBKeepAliveTag, null,
                        new Gson().toJson(request).getBytes());
            }
        };
        return channel.basicConsume(Configuration.KeepAliveQueue, true,
                consumer_keepalive);
    }

    public static void main(String[] argv) throws Exception {
        KeepAliveManager manager =
                new KeepAliveManager(Configuration.RabbitMQHost);
    }
}
