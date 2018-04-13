import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ConnectionManager extends RabbitMQProcess {

    public ConnectionManager(String host) throws IOException, TimeoutException {
        super(host);

        // declare USERS_DB exchange
        channel.exchangeDeclare(Configuration.UsersDBExchange,
                BuiltinExchangeType.DIRECT);

        // declare usersDB responses queue
        channel.queueDeclare(Configuration.ConnMgrUsersDBResponseQueue,
                true, false, false, null);

        // declare RADIOS_DB exchange
        channel.exchangeDeclare(Configuration.RadiosDBExchange,
                BuiltinExchangeType.DIRECT);

        // declare LOGS exchange
        channel.exchangeDeclare(Configuration.LogsExchange,
                BuiltinExchangeType.DIRECT);

        consumeConnections();
        consumeUsersDB();
    }

    private String consumeConnections() throws IOException {
        // CONNECTIONS consumer
        channel.queueDeclare(Configuration.ConnectionsQueue,
                true, false, false, null);
        Consumer consumer_connect = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                String json = new String(body, "UTF-8");
                UserConnectRequest request = new Gson().fromJson(json,
                        UserConnectRequest.class);

                System.out.println(" [x] Received connection request from: "
                        + request.username + " to: " + request.radio);

                // ask usersDB to register connection
                channel.basicPublish(Configuration.UsersDBExchange,
                        Configuration.UsersDBConnectTag, null,
                        new Gson().toJson(request).getBytes());
            }
        };
        return channel.basicConsume(Configuration.ConnectionsQueue,
                true, consumer_connect);
    }

    private String consumeUsersDB() throws IOException {
        // usersDB consume
        Consumer consumer_usersdb = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                String json = new String(body, "UTF-8");
                UserConnectResponse response = new Gson().fromJson(json,
                        UserConnectResponse.class);

                for (UserDisconnectRequest disconn :
                        response.closedConnections) {

                    // register closed connections in radios DB
                    RadiosUpdateRequest radiosRequest =
                            new RadiosUpdateRequest(disconn.radio,
                                    disconn.username);
                    channel.basicPublish(Configuration.RadiosDBExchange,
                            Configuration.RadiosDBDisconnectTag, null,
                            new Gson().toJson(radiosRequest).getBytes());

                    // send disconnects to file logger
                    channel.basicPublish(Configuration.LogsExchange,
                            Configuration.LogsDisconnectionTag, null,
                            disconn.toLogLine().getBytes());
                }

                if (response.couldConnect) {
                    // send connect to file logger
                    channel.basicPublish(Configuration.LogsExchange,
                            Configuration.LogsConnectionTag, null,
                            response.toLogLine().getBytes());

                    // register connection in radios DB
                    RadiosUpdateRequest radioConnectRequest =
                            new RadiosUpdateRequest(response.radio,
                                    response.username);
                    channel.basicPublish(Configuration.RadiosDBExchange,
                            Configuration.RadiosDBConnectTag, null,
                            new Gson().toJson(radioConnectRequest).getBytes());

                    System.out.println(" [X] User: " + response.username +
                            " connected to radio: " + response.radio);
                } else {
                    System.out.println(" [X] User: " + response.username +
                            " denied connection to radio: " + response.radio);
                }

                String jsonResponse = new Gson().toJson(response);
                channel.basicPublish("", response.returnQueueName, null,
                        jsonResponse.getBytes());
            }
        };
        return channel.basicConsume(Configuration.ConnMgrUsersDBResponseQueue,
                true, consumer_usersdb);
    }

    public static void main(String[] argv) throws Exception {

        ConnectionManager manager =
                new ConnectionManager(Configuration.RabbitMQHost);
    }

}
