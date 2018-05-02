import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

// clase para visualizar las estadisticas emitidas por la base de datos de
// radios
public class RadioStatisticsViewer extends RabbitMQProcess {

    public RadioStatisticsViewer(String host) throws IOException,
            TimeoutException {
        super(host);

        // declare RADIOS_STATS exchange
        getChannel().exchangeDeclare(Configuration.RadiosStatisticsExchange,
                BuiltinExchangeType.FANOUT);
    }

    @Override
    public void run() throws IOException {
        consumeStatistics();
    }

    private String consumeStatistics() throws IOException {
        String statisticsQueue = getChannel().queueDeclare().getQueue();
        getChannel().queueBind(statisticsQueue,
                Configuration.RadiosStatisticsExchange, "");

        Consumer consumerStatistics = new DefaultConsumer(getChannel()) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                String json = new String(body, "UTF-8");
                RadiosConnectionsStatistics statistics = new Gson().fromJson
                        (json, RadiosConnectionsStatistics.class);

                System.out.println(" [x] Showing connections per radio: ");
                for (String radio : statistics.getRadioConnections().keySet()) {
                    System.out.println(radio + ": " +
                            statistics.getRadioConnections().get(radio));
                }
            }
        };
        // consume de una cola temporal a traves de un exchange
        // por lo que no tiene sentido ack manual
        return getChannel().basicConsume(statisticsQueue,
                true, consumerStatistics);
    }

    public static void main(String[] argv) throws Exception {
        RadioStatisticsViewer statisticsViewer =
                new RadioStatisticsViewer(Configuration.RabbitMQHost);
        statisticsViewer.run();
    }
}
