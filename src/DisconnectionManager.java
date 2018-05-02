import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

// clase que escucha pedidos de desconexion de los clientes y dirige las
// acciones a tomar en respuesta
public class DisconnectionManager extends RabbitMQProcess {

    public DisconnectionManager(String host) throws IOException,
            TimeoutException {
        super(host);

        // declare USERS_DB exchange
        getChannel().exchangeDeclare(Configuration.UsersDBExchange,
                BuiltinExchangeType.TOPIC);

        // declare RADIOS_DB exchange
        getChannel().exchangeDeclare(Configuration.RadiosDBExchange,
                BuiltinExchangeType.TOPIC);

        // declare LOGS exchange
        getChannel().exchangeDeclare(Configuration.LogsExchange,
                BuiltinExchangeType.DIRECT);
    }

    @Override
    public void run() throws IOException {
        consumeDisconnections();
    }

    private String consumeDisconnections() throws IOException {
        // DISCONNECTIONS consumer
        getChannel().queueDeclare(Configuration.DisconnectionsQueue, true,
                false, false, null);
        Consumer consumer_disconnect = new DefaultConsumer(getChannel()) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                String json = new String(body, "UTF-8");
                UserDisconnectRequest clientRequest = new Gson().fromJson(json,
                        UserDisconnectRequest.class);
                System.out.println(" [x] Received request to disconnect user:" +
                        " " + clientRequest.getUsername() + " from: " +
                        clientRequest.getRadio());

                // assemble database request
                DatabaseRequest dbRequest = new DatabaseRequest
                        (Configuration.UsersTypeDisconnect, json,
                                clientRequest.getUsername());

                // ask usersDB to register disconnection
                getChannel().basicPublish(Configuration.UsersDBExchange,
                        dbRequest.getRoutingKey(), null,
                        new Gson().toJson(dbRequest).getBytes());

                // ask radiosDB to register disconnection
                getChannel().basicPublish(Configuration.RadiosDBExchange,
                        dbRequest.getRoutingKey(), null,
                        new Gson().toJson(dbRequest).getBytes());

                // send disconnects to file logger
                getChannel().basicPublish(Configuration.LogsExchange,
                        Configuration.LogsDisconnectionTag, null,
                        clientRequest.toLogLine().getBytes());

                getChannel().basicAck(envelope.getDeliveryTag(), false);
            }
        };
        return getChannel().basicConsume(Configuration.DisconnectionsQueue,
                false, consumer_disconnect);
    }

    public static void main(String[] argv) throws Exception {
        DisconnectionManager manager =
                new DisconnectionManager(Configuration.RabbitMQHost);
        manager.run();
    }
}
