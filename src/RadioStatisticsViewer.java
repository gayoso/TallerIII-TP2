import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RadioStatisticsViewer extends RabbitMQProcess {

    public RadioStatisticsViewer(String host) throws IOException,
            TimeoutException {
        super(host);

        // declare RADIOS_STATS exchange
        channel.exchangeDeclare(Configuration.RadiosStatisticsExchange,
                BuiltinExchangeType.FANOUT);

        consumeStatistics();
    }

    private String consumeStatistics() throws IOException {
        String statisticsQueue = channel.queueDeclare().getQueue();
        channel.queueBind(statisticsQueue,
                Configuration.RadiosStatisticsExchange, "");

        Consumer consumerStatistics = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                String json = new String(body, "UTF-8");
                RadiosConnectionsStatistics statistics = new Gson().fromJson
                        (json, RadiosConnectionsStatistics.class);

                System.out.println(" [x] Showing connections per radio: ");
                for (String radio : statistics.radioConnections.keySet()) {
                    System.out.println(radio + ": " +
                            statistics.radioConnections.get(radio));
                }
            }
        };
        return channel.basicConsume(statisticsQueue, true, consumerStatistics);
    }

    public static void main(String[] argv) throws Exception {
        RadioStatisticsViewer statisticsViewer =
                new RadioStatisticsViewer(Configuration.RabbitMQHost);
    }
}
