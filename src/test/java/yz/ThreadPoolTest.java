package yz;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.*;

/**
 * author: liuyazong
 * datetime: 2018/3/18 下午12:02
 * <p></p>
 */
@Slf4j
public class ThreadPoolTest {

    @Test
    public void newFixedThreadPool() throws InterruptedException {
        int nThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        CountDownLatch countDownLatch = new CountDownLatch(nThreads);
        for (int i = 0; i < nThreads; i++) {
            int finalI = i;
            executorService.submit(() -> {
                log.debug("{}", finalI);
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
        executorService.shutdown();
    }

    @Test
    public void threadPoolExecutor() throws InterruptedException {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                4,
                8,
                0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(2),
                Executors.defaultThreadFactory(),
                (r, executor) -> log.debug("just ignore {},executor {}", r, executor)) {

            private ThreadLocal<Long> start = new ThreadLocal<>();

            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);
                start.set(System.currentTimeMillis());
                log.debug("before execute thread {}, task {}", t, r);
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                Long startMs = start.get();
                long curMs = System.currentTimeMillis();
                log.debug("after execute thread {}, task {}, time {}ms", t, r, curMs - startMs);
                start.remove();
            }

            @Override
            protected void terminated() {
                super.terminated();
                log.debug("terminated");
            }
        };
        log.debug("executor {}", threadPoolExecutor);
        for (int i = 0; i < 16; i++) {
            threadPoolExecutor.submit(() -> {
                try {
                    Thread.sleep(3000);
                    log.debug("thread {} execute", Thread.currentThread());
                } catch (InterruptedException e) {
                }
            });
        }

        log.debug("executor {}", threadPoolExecutor);
        threadPoolExecutor.shutdown();

        Thread.currentThread().join();
    }

    public void scheduledThreadPoolExecutor() {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(4);

    }

    @Test
    public void threadStarvationDeadLock() throws InterruptedException, ExecutionException {

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                3,
                8,
                0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(2),
                Executors.defaultThreadFactory(),
                (r, executor) -> log.debug("just ignore {},executor {}", r, executor));

        Callable<String> taskA = () -> {
            Callable<String> taskB = () -> "task b";
            Future<String> future = threadPoolExecutor.submit(taskB);
            String resultOfTaskB = future.get();
            log.debug("task b result: {}", resultOfTaskB);
            return "task a ".concat(resultOfTaskB);
        };

        Future<String> future = threadPoolExecutor.submit(taskA);
        String resultOfTaskA = future.get();
        log.debug("task a result: {}", resultOfTaskA);

        threadPoolExecutor.shutdown();

        Thread.currentThread().join();
    }

}
