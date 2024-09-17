package ir.xenoncommunity.utils;

import ir.xenoncommunity.XenonCore;

import java.util.concurrent.*;

@SuppressWarnings("unused")
public class TaskManager {
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;

    public TaskManager() {
        this.executorService = new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors() * 2,
                Runtime.getRuntime().availableProcessors() * 4,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy());

        this.scheduledExecutorService = Executors.newScheduledThreadPool(4);
    }

    public synchronized void add(final Runnable runnableIn) {
        executorService.submit(() -> {
            try {
                runnableIn.run();
            } catch (final Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            }
        });
    }

    public synchronized void repeatingTask(final Runnable runnableIn, final int initDelay, final int delayInMS, final TimeUnit timeUnit) {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                runnableIn.run();
            } catch (final Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            }
        }, initDelay, delayInMS, timeUnit);
    }
    public synchronized void independentTask(final Runnable task) {
        new Thread(() -> {
            try {
                task.run();
            } catch (final Exception e) {
                XenonCore.instance.getLogger().error(e.getMessage());
            }
        }).start();
    }
}
