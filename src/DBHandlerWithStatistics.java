import com.google.gson.Gson;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public abstract class DBHandlerWithStatistics<T extends DatabaseRow>
        extends RabbitMQProcess {

    Database<T> database;
    private ScheduledExecutorService statisticsScheduler = null;
    private List<ScheduledFuture<?>> statisticsHandles = null;

    public DBHandlerWithStatistics(String host, Database database) throws
            IOException,
            TimeoutException {
        super(host);
        this.database = database;

        List<Runnable> statisticTasks = getStatisticsOperations();
        List<Integer> statisticTasksPeriods = getStatisticsPeriodsSeconds();
        if (statisticTasks.size() > 0) {
            statisticsScheduler = Executors
                    .newScheduledThreadPool(1);
            statisticsHandles = new LinkedList<>();

            for (int i = 0; i < statisticTasks.size(); ++i) {
                Runnable r = statisticTasks.get(i);
                int period = statisticTasksPeriods.get(i);
                ScheduledFuture<?> statisticsHandle =
                        statisticsScheduler.scheduleAtFixedRate(r, period,
                                period, TimeUnit.SECONDS);
                statisticsHandles.add(statisticsHandle);
            }
        }
    }

    @Override
    protected void close() throws IOException, TimeoutException {
        super.close();

        if (statisticsScheduler != null) {
            for (ScheduledFuture<?> f : statisticsHandles) {
                f.cancel(true);
            }
            statisticsScheduler.shutdown();
        }
    }

    protected abstract List<Runnable> getStatisticsOperations();

    protected abstract List<Integer> getStatisticsPeriodsSeconds();
}
