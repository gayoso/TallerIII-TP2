
// clase que encapsula una estadistica que se emite periodicamente
// runnable indica que se hace durante el calculo de la estadistica
public class StatisticTask {

    private Runnable runnable;
    private int period;

    public StatisticTask(Runnable runnable, int period) {
        this.runnable = runnable;
        this.period = period;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }
}
