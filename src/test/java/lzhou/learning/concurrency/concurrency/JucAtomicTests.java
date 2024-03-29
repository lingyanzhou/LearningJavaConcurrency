package lzhou.learning.concurrency.concurrency;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description: 原子性数据
 * - 使用CPU指令CAS(Compare and Swap)实现原子性
 * - 相比synchronized block(使用JVM指令monitorenter / monitorexit)更高效
 * <p>
 * [AS原理深度分析及其结合Lock，Atomic分析](https://blog.csdn.net/likailonghaha/article/details/70156858)
 * @author: lingy
 * @Date: 2019-06-28 15:28:11
 * @param: null
 * @return:
 */
public class JucAtomicTests {

    /**
     * @Description: 使用AtomicBoolean实现交替旋转锁
     * @author: lingy
     * @Date: 2019-06-28 16:32:25
     * @param: null
     * @return:
     */
    private static class AlternatingLock {
        private AtomicBoolean atomicBoolean = null;

        public AlternatingLock() {
            atomicBoolean = new AtomicBoolean(true);
        }

        public void aquire(boolean side) {
            while (side != atomicBoolean.get()) {
                //Spin
            }
        }

        public void release(boolean side) {
            atomicBoolean.compareAndSet(side, !side);
        }
    }

    /**
     * @Description: 使用AtomicBoolean实现交替旋转锁
     * @author: lingy
     * @Date: 2019-06-28 16:23:48
     * @param:
     * @return: void
     */
    @Test
    public void testAtomicBooleanAlternatingLock() throws InterruptedException {
        final int steps = 10;
        final int nthreads = 2;
        final boolean ODD = false;
        final boolean EVEN = true;


        //让Runnable1先跑
        AlternatingLock alternatingLock = new AlternatingLock();
        alternatingLock.release(ODD);

        List<Integer> orderList = new ArrayList(steps * 2);
        Runnable printEven = () -> {
            for (int loop = 0; loop < steps; ++loop) {
                alternatingLock.aquire(EVEN);
                System.out.println("EVEN: Value = " + (loop * 2));
                orderList.add(loop * 2);
                alternatingLock.release(EVEN);
            }
        };
        Runnable printOdd = () -> {
            for (int loop = 0; loop < steps; ++loop) {
                alternatingLock.aquire(ODD);
                System.out.println("ODD: Value = " + (loop * 2 + 1));
                orderList.add(loop * 2 + 1);
                alternatingLock.release(ODD);
            }
        };

        ExecutorService executorService = Executors.newFixedThreadPool(nthreads);

        executorService.submit(printEven);
        executorService.submit(printOdd);

        executorService.shutdown();
        executorService.awaitTermination(20, TimeUnit.SECONDS);

        for (int i = 0; i < orderList.size(); ++i) {
            Assert.assertEquals(i, orderList.get(i).longValue());
        }
    }

    /**
     * @Description: 使用AtomicBoolean实现交替旋转锁
     * @author: lingy
     * @Date: 2019-06-28 16:32:25
     * @param: null
     * @return:
     */
    private static class SpinLock {
        private AtomicInteger atomicInteger = null;

        public SpinLock() {
            atomicInteger = new AtomicInteger(0);
        }

        public void lock(int side) {
            while (!atomicInteger.compareAndSet(0, side)) {
                //Spin
            }
        }

        public void unlock(int side) {
            while (!atomicInteger.compareAndSet(side, 0)) {
                //Spin
            }
        }
    }

    /**
     * @Description: 使用AtomicBoolean实现交替旋转锁
     * @author: lingy
     * @Date: 2019-06-28 16:23:48
     * @param:
     * @return: void
     */
    @Test
    public void testAtomicIntegerSpinLock() throws InterruptedException {
        final int steps = 5;
        final int nthreads = 3;

        SpinLock spinLock = new SpinLock();

        List<Integer> orderList = new ArrayList(steps * 2);
        Callable<Integer>[] callables = new Callable[nthreads];
        for (int i = 0; i < nthreads; ++i) {
            int ii = i + 1;
            callables[i] = new Callable() {
                @Override
                public Integer call() throws InterruptedException {
                    for (int loop = 0; loop < steps; ++loop) {
                        spinLock.lock(ii);
                        orderList.add(1);
                        System.out.println(Thread.currentThread().getName());
                        Thread.sleep(ThreadLocalRandom.current().nextInt(1000));
                        orderList.add(0);
                        spinLock.unlock(ii);
                    }
                    return 1;
                }
            };
        }

        ExecutorService executorService = Executors.newFixedThreadPool(nthreads);

        for (Callable callable : callables) {
            executorService.submit(callable);
        }

        executorService.shutdown();
        executorService.awaitTermination(20, TimeUnit.SECONDS);

        for (int i = 0; i < steps; ++i) {
            if (i % 2 == 0) {
                Assert.assertEquals(1L, orderList.get(i).longValue());
            } else {
                Assert.assertEquals(0L, orderList.get(i).longValue());
            }
        }
    }
}
