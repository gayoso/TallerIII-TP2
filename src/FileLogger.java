import com.google.gson.Gson;
import com.rabbitmq.client.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeoutException;

// clase base para un logger a archivos
// solo es necesario decirle a que exchanges bindearse para loggear los
// mensajes que reciba
public abstract class FileLogger extends RabbitMQProcess {

    PrintWriter logWriter;
    String logsQueue;

    public FileLogger(String host, String logFilename) throws
            IOException, TimeoutException {
        super(host);

        // declare LOGS exchange
        getChannel().exchangeDeclare(Configuration.LogsExchange,
                BuiltinExchangeType.DIRECT);

        logWriter = new PrintWriter(new FileWriter(logFilename, true));

        logsQueue = getChannel().queueDeclare().getQueue();
        for (String tag : getBindings()) {
            getChannel().queueBind(logsQueue, Configuration.LogsExchange, tag);
        }
    }

    @Override
    public void run() throws IOException {
        consumeLogs();
    }

    protected abstract List<String> getBindings();

    public String consumeLogs() throws IOException {
        // consume connection logs
        Consumer connectConsumer = new DefaultConsumer(getChannel()) {
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

        // consume de una cola temporal a traves de un exchange
        // por lo que no tiene sentido ack manual
        return getChannel().basicConsume(logsQueue, true, connectConsumer);
    }

    @Override
    protected void close() throws IOException, TimeoutException {
        super.close();
        logWriter.close();
    }
}
