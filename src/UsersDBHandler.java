import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

// base de datos de usuarios
public class UsersDBHandler extends DBHandlerWithStatistics<User> {

    private String queueName;

    public UsersDBHandler(String host, Database<User> database,
                          List<String> masks)
            throws IOException, TimeoutException {
        super(host, database);

        // declare USERS_DB exchange
        getChannel().exchangeDeclare(Configuration.UsersDBExchange,
                BuiltinExchangeType.TOPIC);

        // declare USERS_STATS exchange
        getChannel().exchangeDeclare(Configuration.UsersStatisticsExchange,
                BuiltinExchangeType.FANOUT);

        this.queueName = Configuration.UsersDBExchange + "_" +
                Configuration.maskListToStr(masks);
        getChannel().queueDeclare(queueName, true, false, false, null);
        for (String mask : masks) {
            getChannel().queueBind(queueName,
                    Configuration.UsersDBExchange, mask);
        }
    }

    @Override
    public void run() throws IOException {

        Consumer usersConsumer = new DefaultConsumer(getChannel()) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                // parse request
                String jsonRequest = new String(body, "UTF-8");

                DatabaseRequest request = new Gson().fromJson(jsonRequest,
                        DatabaseRequest.class);

                if (request.getType() == Configuration.UsersTypeConnect) {
                    consumeConnection(request.getSerializedRequest());
                } else if (request.getType() ==
                        Configuration.UsersTypeDisconnect) {
                    consumeDisconnection(request.getSerializedRequest());
                } else if (request.getType() ==
                        Configuration.UsersTypeKeepAlive) {
                    consumeKeepAlive(request.getSerializedRequest());
                } else {
                    Logger.output("Invalid request type received: " +
                    request.getType() + ", request: " +
                            request.getSerializedRequest());
                }

                getChannel().basicAck(envelope.getDeliveryTag(), false);
            }
        };

        getChannel().basicConsume(queueName, false, usersConsumer);
    }

    @Override
    protected List<StatisticTask> getStatistics() {
        List<StatisticTask> operations = new LinkedList<>();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // get statistics
                LimitedSortedSet<UserSecondsListened> usersMostListened =
                        new LimitedSortedSet<>(Configuration.UserStatisticsN,
                                new UsersSecondsListenedComparator());
                for (User row : database.getRows()) {
                    UserSecondsListened userStats =
                            new UserSecondsListened(row.username,
                                    row.secondsListening);
                    usersMostListened.add(userStats);
                }

                UsersSecondsListenedStatistics stats =
                        new UsersSecondsListenedStatistics(usersMostListened);
                String jsonStats = new Gson().toJson(stats);

                try {
                    getChannel().basicPublish(
                            Configuration.UsersStatisticsExchange, "", null,
                            jsonStats.getBytes());
                } catch (IOException e) {
                    Logger.output("IOEXception during statistics publish");
                }
            }
        };

        operations.add(new StatisticTask(runnable,
                Configuration.UsersStatisticsPeriodSeconds));

        return operations;
    }

    public void consumeConnection(String jsonRequest) throws IOException {

        UserConnectRequest request = new Gson().fromJson(jsonRequest,
                UserConnectRequest.class);
        UserConnectResponse response = new UserConnectResponse(request);

        // get user record from DB
        User user = database.getRow(request.getUsername());
        if (user == null) {
            System.out.println("User: " + request.getUsername() + " not " +
                    "found, creating new user");
            user = new User(request.getUsername());
        }
        // first check if any connection is not active and remove it
        int connectionId = 0;
        Date now = new Date();
        ListIterator<UserRadioConnection> connIter =
                user.connections.listIterator();
        while(connIter.hasNext()){
            UserRadioConnection connection = connIter.next();

            Date then = connection.keepAlive;
            if (now.getTime() - then.getTime() >
                    Configuration.SecondsUntilDropConnection * 1000) {
                UserDisconnectRequest disconnectRequest = new
                        UserDisconnectRequest(request.getUsername(),
                        connection.radio,
                        connection.connectionID);
                response.getClosedConnections().add(disconnectRequest);
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
            UserRadioConnection connection =
                    new UserRadioConnection(response.getRadio(),
                            new Date(), connectionId);
            user.connections.add(connection);
            response.setCouldConnect(true);
            response.setConnectionId(connectionId);

            System.out.println(" [x] User: " + user.username +
                    " connected to: " + request.getRadio());
        } else {
            response.setCouldConnect(false);

            System.out.println(" [x] User: " + user.username +
                    " not connected to: " + request.getRadio());
        }
        // update user
        database.updateRow(user);

        String jsonResponse = new Gson().toJson(response);
        getChannel().basicPublish("",
                Configuration.ConnMgrUsersDBResponseQueue, null,
                jsonResponse.getBytes());
    }

    public void consumeDisconnection(String jsonRequest) throws IOException {

        UserDisconnectRequest request = new Gson().fromJson(jsonRequest,
                UserDisconnectRequest.class);

        // get user record from DB
        User user = database.getRow(request.getUsername());
        if (user != null) {
            ListIterator<UserRadioConnection> connIter =
                    user.connections.listIterator();
            while(connIter.hasNext()){
                UserRadioConnection connection = connIter.next();
                if (connection.connectionID == request.getConnectionId() &&
                        connection.radio.equals(request.getRadio())) {
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

    public void consumeKeepAlive(String jsonRequest) throws IOException {

        KeepAliveRequest request = new Gson().fromJson(jsonRequest,
                KeepAliveRequest.class);

        // get user record from DB
        User user = database.getRow(request.getUsername());
        if (user == null) {
            System.out.println(" [x] Error: user who sent keep alive " +
                    "does not exist");
            return;
        }

        // refresh keep alive
        ListIterator<UserRadioConnection> connIter =
                user.connections.listIterator();
        while(connIter.hasNext()){
            UserRadioConnection connection = connIter.next();
            if (connection.connectionID == request.getConnectionId() &&
                    connection.radio.equals(request.getRadio())) {
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

    public static void main(String[] argv) throws Exception {
        if (argv.length < 1) {
            System.out.println("Usage: UsersDBHAndler mask1 mask2 mask3");
            return;
        }
        List<String> masks = new LinkedList<>(Arrays.asList(argv));

        // define database
        Database<User> database = new DatabaseJson<>(
                Configuration.UsersDBExchange + "_" +
                        Configuration.maskListToStr(masks), User.class);

        // start database handler
        UsersDBHandler handler = new UsersDBHandler(Configuration.RabbitMQHost,
                database, masks);
        handler.run();
    }
}
