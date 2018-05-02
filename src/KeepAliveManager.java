import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

// clase que escucha mensajes de keep alive de los clientes
// y lo comunica al resto del sistema
public class KeepAliveManager extends RabbitMQProcess {

    public KeepAliveManager(String host) throws IOException, TimeoutException {
        super(host);

        // declare USERS_DB exchange
        getChannel().exchangeDeclare(Configuration.UsersDBExchange,
                BuiltinExchangeType.TOPIC);
    }

    @Override
    public void run() throws IOException {
        consumeKeepAlives();
    }

    private String consumeKeepAlives() throws IOException {
        // KEEP ALIVE consumer
        getChannel().queueDeclare(Configuration.KeepAliveQueue, true,
                false, false, null);
        Consumer consumer_keepalive = new DefaultConsumer(getChannel()) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                String json = new String(body, "UTF-8");
                KeepAliveRequest clientRequest = new Gson().fromJson(json,
                        KeepAliveRequest.class);
                System.out.println(" [x] Received keep alive request from: "
                        + clientRequest.getUsername() + " to: " +
                        clientRequest.getRadio() + " id: " +
                        clientRequest.getConnectionId());

                // ask usersDB to register connection
                DatabaseRequest usersdbRequest = new DatabaseRequest
                        (Configuration.UsersTypeKeepAlive, json,
                                clientRequest.getUsername());
                getChannel().basicPublish(Configuration.UsersDBExchange,
                        usersdbRequest.getRoutingKey(), null,
                        new Gson().toJson(usersdbRequest).getBytes());

                getChannel().basicAck(envelope.getDeliveryTag(), false);
            }
        };
        return getChannel().basicConsume(Configuration.KeepAliveQueue, false,
                consumer_keepalive);
    }

    public static void main(String[] argv) throws Exception {
        KeepAliveManager manager =
                new KeepAliveManager(Configuration.RabbitMQHost);
        manager.run();
    }
}
