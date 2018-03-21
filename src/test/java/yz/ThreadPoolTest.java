package yz;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

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

    @Test
    public void threadPoolExecutorExtends() throws InterruptedException {

        //扩展ThreadPoolExecutor
        class ExtThreadPoolExecutor extends ThreadPoolExecutor {

            //记录线程开始执行的时间
            private ThreadLocal<Long> startThreadLocal = new ThreadLocal<>();
            //记录线程执行的总时间
            private AtomicLong totalTime = new AtomicLong(0);
            //记录已执行的线程数
            private AtomicLong totalCount = new AtomicLong(0);

            public ExtThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
                super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
            }

            public ExtThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
                super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
            }

            public ExtThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
                super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
            }

            public ExtThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
                super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
            }

            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);
                startThreadLocal.set(System.currentTimeMillis());
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                if (t == null) {
                    Long start = startThreadLocal.get();
                    startThreadLocal.remove();
                    long thisTime = System.currentTimeMillis() - start;
                    long count = totalCount.addAndGet(1);
                    long total = totalTime.addAndGet(thisTime);
                    log.debug("task {} taking {} ms, average taking {} ms", r, thisTime, total / count);
                } else {
                    log.error("task {} exception {}", r, t);
                }
            }

            @Override
            protected void terminated() {
                super.terminated();
                log.debug("threadPoolExecutor {} terminated", this);
            }

        }

        //测试自己扩展的ExtThreadPoolExecutor
        int nThreads = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor threadPoolExecutor = new ExtThreadPoolExecutor(
                nThreads,
                nThreads << 1,
                0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
        for (int i = 0; i < nThreads << 1; i++) {
            threadPoolExecutor.submit(() -> {
                try {
                    Thread.sleep(new Random().nextInt(3000));
                    log.debug("current time: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        threadPoolExecutor.shutdown();

        Thread.currentThread().join();

    }

}
