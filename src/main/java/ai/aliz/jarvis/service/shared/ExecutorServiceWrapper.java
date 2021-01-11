package ai.aliz.jarvis.service.shared;

import lombok.SneakyThrows;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class ExecutorServiceWrapper {
    
    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    @SneakyThrows
    private void checkAllFuturesOk(List<? extends Future<?>> futures) {
        for (Future<?> future : futures) {
            // Will throw an exception if anything goes wrong
            future.get();
        }
    }
    
    @SneakyThrows
    public <T> void executeRunnablesInParallel(Collection<Runnable> tasks,
                                               long timeout,
                                               TimeUnit unit) {
        
        List<Callable<Object>> callableTasks = tasks.stream()
                                                    .map(Executors::callable)
                                                    .collect(Collectors.toList());
        
        List<Future<Object>> futures = executorService.invokeAll(callableTasks, timeout, unit);
        checkAllFuturesOk(futures);
    }
}
