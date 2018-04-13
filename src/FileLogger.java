import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeoutException;

public abstract class FileLogger extends RabbitMQProcess {

    PrintWriter logWriter;
    String logsQueue;

    public FileLogger(String host, String logFilename) throws
            IOException, TimeoutException {
        super(host);

        // declare LOGS exchange
        channel.exchangeDeclare(Configuration.LogsExchange,
                BuiltinExchangeType.DIRECT);

        logWriter = new PrintWriter(new FileWriter(logFilename, true));

        logsQueue = channel.queueDeclare().getQueue();
        for (String tag : getBindings()) {
            channel.queueBind(logsQueue, Configuration.LogsExchange, tag);
        }
        consumeLogs();
    }

    protected abstract List<String> getBindings();

    public String consumeLogs() throws IOException {
        // consume connection logs
        Consumer connectConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {

                // write log to file
                String logLine = new String(body, "UTF-8");
                logWriter.println(logLine);

                System.out.println(" [x] Received: " + logLine);
            }
        };
        return channel.basicConsume(logsQueue, true, connectConsumer);
    }

    @Override
    protected void close() throws IOException, TimeoutException {
        super.close();
        logWriter.close();
    }
}
