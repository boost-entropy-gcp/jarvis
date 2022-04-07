package ai.aliz.jarvis.service.shared;

import lombok.SneakyThrows;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import com.google.common.collect.Collections2;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import org.springframework.stereotype.Service;

@Service
public class ExecutorServiceWrapper {
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    @SneakyThrows
    public <T> void executeCallablesInParallel(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
        executorService.invokeAll(tasks, timeout, unit).forEach(Futures::getUnchecked);
    }
    
    @SneakyThrows
    public void executeRunnablesInParallel(Collection<Runnable> tasks, long timeout, TimeUnit unit) {
        executeCallablesInParallel(Collections2.transform(tasks, Executors::callable), timeout, unit);
    }
    
    @PreDestroy
    public void preDestroy() {
        MoreExecutors.shutdownAndAwaitTermination(executorService, 1, TimeUnit.DAYS);
    }
}
