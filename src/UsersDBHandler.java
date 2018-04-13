import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeoutException;

public class UsersDBHandler extends DBHandlerWithStatistics<UsersDBRow> {

    public UsersDBHandler(String host, Database<UsersDBRow> database) throws
            IOException,
            TimeoutException {
        super(host, database);

        // declare USERS_DB exchange
        channel.exchangeDeclare(Configuration.UsersDBExchange,
                BuiltinExchangeType.DIRECT);

        // declare USERS_STATS exchange
        channel.exchangeDeclare(Configuration.UsersStatisticsExchange,
                BuiltinExchangeType.FANOUT);

        // consumer methods
        consumeConnections();
        consumeDisconnections();
        consumeKeepAlive();
    }

    @Override
    protected List<Runnable> getStatisticsOperations() {
        List<Runnable> operations = new LinkedList<>();
        operations.add(new Runnable() {
            @Override
            public void run() {
                // get statistics
                LimitedSortedSet<UserSecondsListened> usersMostListened =
                        new LimitedSortedSet<>(Configuration.UserStatisticsN,
                                new UsersSecondsListenedComparator());
                for (UsersDBRow row : database.getRows()) {
                    UserSecondsListened userStats =
                            new UserSecondsListened(row.username,
                                    row.secondsListening);
                    usersMostListened.add(userStats);
                }

                UsersSecondsListenedStatistics stats =
                        new UsersSecondsListenedStatistics(usersMostListened);
                String jsonStats = new Gson().toJson(stats);

                try {
                    channel.basicPublish(Configuration.UsersStatisticsExchange,
                            "", null, jsonStats.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        return operations;
    }

    @Override
    protected List<Integer> getStatisticsPeriodsSeconds() {
        List<Integer> operationsPeriods = new LinkedList<>();
        operationsPeriods.add(Configuration.UsersStatisticsPeriodSeconds);
        return operationsPeriods;
    }

    public String consumeConnections() throws IOException {
        // consume CONNECT_TO_RADIO requests
        String connectUsersQueue = channel.queueDeclare().getQueue();
        channel.queueBind(connectUsersQueue, Configuration.UsersDBExchange,
                Configuration.UsersDBConnectTag);
        Consumer connectConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                // parse request
                String jsonRequest = new String(body, "UTF-8");

                UserConnectRequest request = new Gson().fromJson(jsonRequest,
                        UserConnectRequest.class);
                UserConnectResponse response = new UserConnectResponse(request);

                // get user record from DB
                UsersDBRow user = database.getRow(request.username);
                if (user == null) {
                    System.out.println("User: " + request.username + " not " +
                            "found, creating new user");
                    user = new UsersDBRow(request.username);
                }
                // first check if any connection is not active and remove it
                int connectionId = 0;
                Date now = new Date();
                ListIterator<UsersDBRowRadioConnection> connIter =
                        user.connections.listIterator();
                while(connIter.hasNext()){
                    UsersDBRowRadioConnection connection = connIter.next();

                    Date then = connection.keepAlive;
                    if (now.getTime() - then.getTime() >
                            Configuration.SecondsUntilDropConnection * 1000) {
                        UserDisconnectRequest disconnectRequest = new
                                UserDisconnectRequest(request.username,
                                connection.radio,
                                connection.connectionID);
                        response.closedConnections.add(disconnectRequest);
                        connIter.remove();

                        System.out.println(" [x] Closing old connection to: " +
                                connection.radio);
                    } else {
                        if (connection.connectionID > connectionId) {
                            connectionId = connection.connectionID;
                        }
                    }
                }
                connectionId = (connectionId + 1) %
                        Configuration.MaxConnectionsPerUnlimitedUser;
                // then check if user can connect to radio
                if (user.connections.size() < user.connectionsLimit) {
                    UsersDBRowRadioConnection connection =
                            new UsersDBRowRadioConnection(response.radio,
                                    new Date(), connectionId);
                    user.connections.add(connection);
                    response.couldConnect = true;
                    response.connectionId = connectionId;

                    System.out.println(" [x] User: " + user.username +
                            " connected to: " + request.radio);
                } else {
                    response.couldConnect = false;

                    System.out.println(" [x] User: " + user.username +
                            " not connected to: " + request.radio);
                }
                // update user
                database.updateRow(user);

                String jsonResponse = new Gson().toJson(response);
                channel.basicPublish("",
                        Configuration.ConnMgrUsersDBResponseQueue, null,
                        jsonResponse.getBytes());
            }
        };
        return channel.basicConsume(connectUsersQueue, true, connectConsumer);
    }

    public String consumeDisconnections() throws IOException {
        // consume DISCONNECT_FROM_RADIO requests
        String disconnectUsersQueue = channel.queueDeclare().getQueue();
        channel.queueBind(disconnectUsersQueue, Configuration.UsersDBExchange,
                Configuration.UsersDBDisconnectTag);
        Consumer disconnectConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                // parse request
                String jsonRequest = new String(body, "UTF-8");

                UserDisconnectRequest request = new Gson().fromJson(jsonRequest,
                        UserDisconnectRequest.class);

                // get user record from DB
                UsersDBRow user = database.getRow(request.username);
                if (user != null) {
                    ListIterator<UsersDBRowRadioConnection> connIter =
                            user.connections.listIterator();
                    while(connIter.hasNext()){
                        UsersDBRowRadioConnection connection = connIter.next();
                        if (connection.connectionID == request.connectionId &&
                                connection.radio.equals(request.radio)) {
                            connIter.remove();
                            System.out.println(" [x] Removing connection " +
                                    "from: " + user.username + " to radio: "
                                    + connection.radio);
                            break;
                        }
                    }
                }
                // update user
                database.updateRow(user);
            }
        };
        return channel.basicConsume(disconnectUsersQueue, true,
                disconnectConsumer);
    }

    public String consumeKeepAlive() throws IOException {
        // consume KEEP_ALIVE requests
        String keepaliveUsersQueue = channel.queueDeclare().getQueue();
        channel.queueBind(keepaliveUsersQueue, Configuration.UsersDBExchange,
                Configuration.UsersDBKeepAliveTag);
        Consumer keepaliveConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                // parse request
                String jsonRequest = new String(body, "UTF-8");

                KeepAliveRequest request = new Gson().fromJson(jsonRequest,
                        KeepAliveRequest.class);

                // get user record from DB
                UsersDBRow user = database.getRow(request.username);
                if (user == null) {
                    System.out.println(" [x] Error: user who sent keep alive " +
                            "does not exist");
                    return;
                }

                // refresh keep alive
                ListIterator<UsersDBRowRadioConnection> connIter =
                        user.connections.listIterator();
                while(connIter.hasNext()){
                    UsersDBRowRadioConnection connection = connIter.next();
                    if (connection.connectionID == request.connectionId &&
                            connection.radio.equals(request.radio)) {
                        connection.keepAlive = new Date();
                        connIter.set(connection);
                        System.out.println(" [x] Refreshing keepalive " +
                                "from: " + user.username + " to radio: "
                                + connection.radio + " id: " +
                                connection.connectionID);
                        break;
                    }
                }

                // add to user total listened minutes
                user.secondsListening += Configuration.KeepAlivePeriodSeconds;

                // update user
                database.updateRow(user);
            }
        };
        return channel.basicConsume(keepaliveUsersQueue, true,
                keepaliveConsumer);
    }

    public static void main(String[] argv) throws Exception {

        // define database
        Database<UsersDBRow> database = new DatabaseRAM();

        // start database handler
        UsersDBHandler handler = new UsersDBHandler(Configuration.RabbitMQHost,
                database);
    }
}
