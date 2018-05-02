import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

// clase para visualizar las estadisticas periodicas que emite la base de
// datos de usuarios
public class UserStatisticsViewer extends RabbitMQProcess {

    public UserStatisticsViewer(String host) throws IOException, TimeoutException {
        super(host);

        // declare USERS_STATS exchange
        getChannel().exchangeDeclare(Configuration.UsersStatisticsExchange,
                BuiltinExchangeType.FANOUT);
    }

    @Override
    public void run() throws IOException {
        consumeStatistics();
    }

    private String consumeStatistics() throws IOException {
        String statisticsQueue = getChannel().queueDeclare().getQueue();
        getChannel().queueBind(statisticsQueue,
                Configuration.UsersStatisticsExchange, "");

        Consumer consumerStatistics = new DefaultConsumer(getChannel()) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                String json = new String(body, "UTF-8");
                UsersSecondsListenedStatistics statistics = new Gson().fromJson
                        (json, UsersSecondsListenedStatistics.class);

                System.out.println(" [x] Showing users who listened most: ");
                for (UserSecondsListened userStats :
                        statistics.getUsersMostListenedSeconds()) {
                    System.out.println(userStats.getUsername() + ": " +
                            userStats.getSecondsListened());
                }
            }
        };

        // consume de una cola temporal a traves de un exchange
        // por lo que no tiene sentido ack manual
        return getChannel().basicConsume(statisticsQueue, true,
                consumerStatistics);
    }

    public static void main(String[] argv) throws Exception {
        UserStatisticsViewer statisticsViewer =
                new UserStatisticsViewer(Configuration.RabbitMQHost);
        statisticsViewer.run();
    }
}
