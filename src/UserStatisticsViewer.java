import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class UserStatisticsViewer extends RabbitMQProcess {

    public UserStatisticsViewer(String host) throws IOException, TimeoutException {
        super(host);

        // declare USERS_STATS exchange
        channel.exchangeDeclare(Configuration.UsersStatisticsExchange,
                BuiltinExchangeType.FANOUT);

        consumeStatistics();
    }

    private String consumeStatistics() throws IOException {
        String statisticsQueue = channel.queueDeclare().getQueue();
        channel.queueBind(statisticsQueue,
                Configuration.UsersStatisticsExchange, "");

        Consumer consumerStatistics = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                String json = new String(body, "UTF-8");
                UsersSecondsListenedStatistics statistics = new Gson().fromJson
                        (json, UsersSecondsListenedStatistics.class);

                System.out.println(" [x] Showing connections per radio: ");
                for (UserSecondsListened userStats :
                        statistics.usersMostListenedSeconds) {
                    System.out.println(userStats.username + ": " +
                            userStats.secondsListened);
                }
            }
        };
        return channel.basicConsume(statisticsQueue, true, consumerStatistics);
    }

    public static void main(String[] argv) throws Exception {
        UserStatisticsViewer statisticsViewer =
                new UserStatisticsViewer(Configuration.RabbitMQHost);
    }
}
