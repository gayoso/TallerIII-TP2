import com.google.gson.Gson;
import com.rabbitmq.client.*;
import sun.security.krb5.Config;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

// clase que recibe de la base de datos de usuarios avisos de conexiones
// exitosas y dirige las acciones a tomar a continuacion
public class ConnectionManager extends RabbitMQProcess {

    public ConnectionManager(String host) throws IOException, TimeoutException {
        super(host);

        // declare USERS_DB exchange
        getChannel().exchangeDeclare(Configuration.UsersDBExchange,
                BuiltinExchangeType.TOPIC);

        // declare usersDB responses queue
        getChannel().queueDeclare(Configuration.ConnMgrUsersDBResponseQueue,
                true, false, false, null);

        // declare RADIOS_DB exchange
        getChannel().exchangeDeclare(Configuration.RadiosDBExchange,
                BuiltinExchangeType.TOPIC);

        // declare LOGS exchange
        getChannel().exchangeDeclare(Configuration.LogsExchange,
                BuiltinExchangeType.DIRECT);
    }

    @Override
    public void run() throws IOException {
        //consumeConnections();
        consumeUsersDB();
    }

    private String consumeUsersDB() throws IOException {
        // usersDB consume
        Consumer consumer_usersdb = new DefaultConsumer(getChannel()) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                String json = new String(body, "UTF-8");
                UserConnectResponse response = new Gson().fromJson(json,
                        UserConnectResponse.class);

                for (UserDisconnectRequest disconn :
                        response.getClosedConnections()) {

                    // register closed connections in radios DB
                    DatabaseRequest dbRequest = new DatabaseRequest
                            (Configuration.UsersTypeDisconnect, new Gson()
                                    .toJson(disconn), disconn.getUsername());
                    getChannel().basicPublish(Configuration.RadiosDBExchange,
                            dbRequest.getRoutingKey(), null,
                            new Gson().toJson(dbRequest).getBytes());

                    // send disconnects to file logger
                    getChannel().basicPublish(Configuration.LogsExchange,
                            Configuration.LogsDisconnectionTag, null,
                            disconn.toLogLine().getBytes());
                }

                if (response.isCouldConnect()) {
                    // send connect to file logger
                    getChannel().basicPublish(Configuration.LogsExchange,
                            Configuration.LogsConnectionTag, null,
                            response.toLogLine().getBytes());

                    // register connection in radios DB
                    DatabaseRequest dbRequest = new DatabaseRequest
                            (Configuration.UsersTypeConnect, new Gson()
                                    .toJson(new UserConnectRequest(response)),
                                    response.getUsername());
                    getChannel().basicPublish(Configuration.RadiosDBExchange,
                            dbRequest.getRoutingKey(), null,
                            new Gson().toJson(dbRequest).getBytes());

                    System.out.println(" [X] User: " + response.getUsername() +
                            " connected to radio: " + response.getRadio());
                } else {
                    System.out.println(" [X] User: " + response.getUsername() +
                            " denied connection to radio: " + response.getRadio());
                }

                String jsonResponse = new Gson().toJson(response);
                getChannel().basicPublish("", response.getReturnQueueName(), null,
                        jsonResponse.getBytes());

                getChannel().basicAck(envelope.getDeliveryTag(), false);
            }
        };
        return getChannel().basicConsume(Configuration.ConnMgrUsersDBResponseQueue,
                false, consumer_usersdb);
    }

    public static void main(String[] argv) throws Exception {

        ConnectionManager manager =
                new ConnectionManager(Configuration.RabbitMQHost);
        manager.run();
    }

}
