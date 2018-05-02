import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

// base de datos distribuida de radios. recibe una serie de mascaras para
// sharding y el objeto base de datos con quien interactuar
public class RadiosDBHandler extends DBHandlerWithStatistics<Radio> {

    private String queueName;

    public RadiosDBHandler(String host, Database<Radio> database,
                           List<String> masks)
            throws IOException, TimeoutException {
        super(host, database);
        this.database = database;

        // declare RADIOS_DB exchange
        getChannel().exchangeDeclare(Configuration.RadiosDBExchange,
                BuiltinExchangeType.TOPIC);

        // declare RADIOS_STATS exchange
        getChannel().exchangeDeclare(Configuration.RadiosStatisticsExchange,
                BuiltinExchangeType.FANOUT);

        this.queueName = Configuration.RadiosDBExchange + "_" +
                Configuration.maskListToStr(masks);
        getChannel().queueDeclare(queueName, true, false, false, null);
        for (String mask : masks) {
            getChannel().queueBind(queueName,
                    Configuration.RadiosDBExchange, mask);
        }
    }

    @Override
    public void run() throws IOException {

        Consumer radiosConsumer = new DefaultConsumer(getChannel()) {
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
                } else {
                    Logger.output("Invalid request type received: " +
                            request.getType() + ", request: " +
                            request.getSerializedRequest());
                }

                getChannel().basicAck(envelope.getDeliveryTag(), false);
            }
        };

        getChannel().basicConsume(queueName, false, radiosConsumer);
    }

    @Override
    protected List<StatisticTask> getStatistics() {
        List<StatisticTask> operations = new LinkedList<>();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // get statistics
                RadiosConnectionsStatistics stats = new RadiosConnectionsStatistics();
                for (Radio row : database.getRows()) {
                    stats.getRadioConnections().put(row.getName(), row.getConnectedUsers());
                }
                String jsonStats = new Gson().toJson(stats);

                try {
                    getChannel().basicPublish(
                            Configuration.RadiosStatisticsExchange, "",
                            null, jsonStats.getBytes());
                } catch (IOException e) {
                    Logger.output("IOEXception during statistics publish");
                }
            }
        };

        operations.add(new StatisticTask(runnable,
                Configuration.RadioStatisticsPeriodSeconds));

        return operations;
    }

    private void consumeConnection(String jsonRequest) throws IOException {

        UserConnectRequest request = new Gson().fromJson(jsonRequest,
                UserConnectRequest.class);

        // get radio record from DB
        Radio radio = database.getRow(request.getRadio());
        if (radio == null) {
            radio = new Radio(request.getRadio());
        }

        // add one connection to counter
        radio.setConnectedUsers(radio.getConnectedUsers() + 1);
        System.out.println(" [x] Adding one connection to radio: " +
                radio.getName());

        // save changes to db
        database.updateRow(radio);
    }

    private void consumeDisconnection(String jsonRequest) throws IOException {

        UserDisconnectRequest request = new Gson().fromJson(jsonRequest,
                UserDisconnectRequest.class);

        // get radio record from DB
        Radio radio = database.getRow(request.getRadio());
        if (radio == null) {
            radio = new Radio(request.getRadio());
        }

        // add one connection to counter
        radio.setConnectedUsers(radio.getConnectedUsers() - 1);
        System.out.println(" [x] Removing one connection from " +
                "radio: " + radio.getName());

        // save changes to db
        database.updateRow(radio);
    }

    public static void main(String[] argv) throws Exception {
        if (argv.length < 1) {
            System.out.println("Usage: UsersDBHAndler mask1 mask2 mask3");
            return;
        }
        List<String> masks = new LinkedList<>(Arrays.asList(argv));

        // define database
        Database<Radio> database = new DatabaseJson<>(
                Configuration.RadiosDBExchange + "_" +
                        Configuration.maskListToStr(masks), Radio.class);

        // start database handler
        RadiosDBHandler handler = new RadiosDBHandler(Configuration.RabbitMQHost,
                database, masks);
        handler.run();
    }
}
