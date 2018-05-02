import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

// clase que abstrae una base de datos con capacidad de lanzar estadisticas
// periodicamente
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

        List<StatisticTask> statisticTasks = getStatistics();
        //List<Integer> statisticTasksPeriods = getStatisticsPeriodsSeconds();
        if (statisticTasks.size() > 0) {
            statisticsScheduler = Executors
                    .newScheduledThreadPool(
                            Configuration.PoolSizeForDBstatistics);
            statisticsHandles = new LinkedList<>();

            for (StatisticTask task : statisticTasks) {
                Runnable r = task.getRunnable();
                int period = task.getPeriod();
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

    protected abstract List<StatisticTask> getStatistics();
}
