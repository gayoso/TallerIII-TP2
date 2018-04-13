import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class RadiosDBHandler extends DBHandlerWithStatistics<RadiosDBRow> {

    public RadiosDBHandler(String host, Database<RadiosDBRow> database) throws
            IOException,
            TimeoutException {
        super(host, database);
        this.database = database;

        // declare RADIOS_DB exchange
        channel.exchangeDeclare(Configuration.RadiosDBExchange,
                BuiltinExchangeType.DIRECT);

        // declare RADIOS_STATS exchange
        channel.exchangeDeclare(Configuration.RadiosStatisticsExchange,
                BuiltinExchangeType.FANOUT);

        consumeConnections();
        consumeDisconnections();
    }

    @Override
    protected List<Runnable> getStatisticsOperations() {
        List<Runnable> operations = new LinkedList<>();
        operations.add(new Runnable() {
            @Override
            public void run() {
                // get statistics
                RadiosConnectionsStatistics stats = new RadiosConnectionsStatistics();
                for (RadiosDBRow row : database.getRows()) {
                    stats.radioConnections.put(row.name, row.connectedUsers);
                }
                String jsonStats = new Gson().toJson(stats);

                try {
                    channel.basicPublish(Configuration.RadiosStatisticsExchange, "",
                            null, jsonStats.getBytes());
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
        operationsPeriods.add(Configuration.RadioStatisticsPeriodSeconds);
        return operationsPeriods;
    }

    private String consumeConnections() throws IOException {
        // consume CONNECT_TO_RADIO requests
        String connectUsersQueue = channel.queueDeclare().getQueue();
        channel.queueBind(connectUsersQueue, Configuration.RadiosDBExchange,
                Configuration.RadiosDBConnectTag);
        Consumer connectConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                // parse request
                String jsonRequest = new String(body, "UTF-8");

                RadiosUpdateRequest request = new Gson().fromJson(jsonRequest,
                        RadiosUpdateRequest.class);

                // get radio record from DB
                RadiosDBRow radio = database.getRow(request.radio);
                if (radio == null) {
                    radio = new RadiosDBRow(request.radio);
                }

                // add one connection to counter
                radio.connectedUsers++;
                System.out.println(" [x] Adding one connection to radio: " +
                        radio.name);

                // save changes to db
                database.updateRow(radio);
            }
        };
        return channel.basicConsume(connectUsersQueue, true, connectConsumer);
    }

    private String consumeDisconnections() throws IOException {
        // consume DISCONNECT_FROM_RADIO requests
        String disconnectUsersQueue = channel.queueDeclare().getQueue();
        channel.queueBind(disconnectUsersQueue, Configuration.RadiosDBExchange,
                Configuration.RadiosDBDisconnectTag);
        Consumer disconnectConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                // parse request
                String jsonRequest = new String(body, "UTF-8");

                RadiosUpdateRequest request = new Gson().fromJson(jsonRequest,
                        RadiosUpdateRequest.class);

                // get radio record from DB
                RadiosDBRow radio = database.getRow(request.radio);
                if (radio == null) {
                    radio = new RadiosDBRow(request.radio);
                }

                // add one connection to counter
                radio.connectedUsers--;
                System.out.println(" [x] Removing one connection from " +
                        "radio: " + radio.name);

                // save changes to db
                database.updateRow(radio);

            }
        };
        return channel.basicConsume(disconnectUsersQueue, true,
                disconnectConsumer);
    }

    public static void main(String[] argv) throws Exception {
        // define database
        Database<RadiosDBRow> database = new DatabaseRAM();

        // start database handler
        RadiosDBHandler handler = new RadiosDBHandler(Configuration.RabbitMQHost,
                database);
    }
}
