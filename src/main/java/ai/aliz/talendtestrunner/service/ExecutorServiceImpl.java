package ai.aliz.talendtestrunner.service;

import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ExecutorServiceImpl {

    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @SneakyThrows
    private void checkAllFuturesOk(List<? extends Future<? extends Object>> futures) {
        for (Future<?> future : futures) {
            // Will throw an exception if anything goes wrong
            future.get();
        }
    }

    @SneakyThrows
    public <T> void executeCallablesInParallel(Collection<? extends Callable<T>> tasks,
                                               long timeout, TimeUnit unit) {
        List<Future<T>> futures = executorService.invokeAll(tasks, timeout, unit);
        checkAllFuturesOk(futures);
    }

    @SneakyThrows
    public <T> void executeRunnablesInParallel(Collection<Runnable> tasks,
                                               long timeout, TimeUnit unit) {
        List<Callable<Object>> callableTasks = tasks.stream()
                .map(Executors::callable)
                .collect(Collectors.toList());
        List<Future<Object>> futures = executorService.invokeAll(callableTasks, timeout, unit);
        checkAllFuturesOk(futures);
    }
}
