package yz;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * author: liuyazong
 * datetime: 2018/3/18 上午11:27
 * <p></p>
 */
@Slf4j
public class ThreadTest {

    @Test
    public void test0() throws InterruptedException {
        Thread thread = new Thread() {
            @Override
            public void run() {
                log.debug("thread test");
            }
        };
        thread.start();
        thread.join();
        log.debug("thread {} finished, thread {} go on.", thread, Thread.currentThread());
    }

    @Test
    public void test1() throws InterruptedException {
        Runnable runnable = () -> {
            log.debug("thread runnable test");
        };
        Thread thread = new Thread(runnable);
        thread.start();
        thread.join();
        log.debug("thread {} finished, thread {} go on.", thread, Thread.currentThread());
    }


}
