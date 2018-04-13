import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class DisconnectionManager extends RabbitMQProcess {

    public DisconnectionManager(String host) throws IOException,
            TimeoutException {
        super(host);

        // declare USERS_DB exchange
        channel.exchangeDeclare(Configuration.UsersDBExchange,
                BuiltinExchangeType.DIRECT);

        // declare RADIOS_DB exchange
        channel.exchangeDeclare(Configuration.RadiosDBExchange,
                BuiltinExchangeType.DIRECT);

        // declare LOGS exchange
        channel.exchangeDeclare(Configuration.LogsExchange,
                BuiltinExchangeType.DIRECT);

        consumeDisconnections();
    }

    private String consumeDisconnections() throws IOException {
        // DISCONNECTIONS consumer
        channel.queueDeclare(Configuration.DisconnectionsQueue, true, false, false,
                null);
        Consumer consumer_disconnect = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                String json = new String(body, "UTF-8");
                UserDisconnectRequest request = new Gson().fromJson(json,
                        UserDisconnectRequest.class);

                System.out.println(" [x] Received request to disconnect user:" +
                        " " + request.username + " from: " + request.radio);

                // ask usersDB to register disconnection
                channel.basicPublish(Configuration.UsersDBExchange,
                        Configuration.UsersDBDisconnectTag, null,
                        new Gson().toJson(request).getBytes());

                // ask radiosDB to register disconnection
                channel.basicPublish(Configuration.RadiosDBExchange,
                        Configuration.RadiosDBDisconnectTag, null,
                        new Gson().toJson(request).getBytes());

                // send disconnects to file logger
                channel.basicPublish(Configuration.LogsExchange,
                        Configuration.LogsDisconnectionTag, null,
                        request.toLogLine().getBytes());
            }
        };
        return channel.basicConsume(Configuration.DisconnectionsQueue, true,
                consumer_disconnect);
    }

    public static void main(String[] argv) throws Exception {
        DisconnectionManager manager =
                new DisconnectionManager(Configuration.RabbitMQHost);
    }
}
