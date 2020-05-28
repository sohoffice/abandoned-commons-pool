package sohoffice.example.abandonedpool;

import static org.junit.Assert.*;

import java.io.PrintWriter;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonsPoolTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final Logger logger = LoggerFactory.getLogger(CommonsPoolTest.class.getName());
  private GenericObjectPool<Integer> goodPool;
  private GenericObjectPool<Integer> recoverablePool;
  private GenericObjectPool<Integer> nonRecoverablePool;

  @Before
  public void setUp() throws Exception {
    GenericObjectPoolConfig config = new GenericObjectPoolConfig();
    config.setMinIdle(1);
    config.setMaxIdle(5);
    config.setMaxTotal(5);
    config.setBlockWhenExhausted(true);
    config.setMaxWaitMillis(100);

    // configure eviction is recommended but is not necessary
    config.setTimeBetweenEvictionRunsMillis(10);
    config.setMinEvictableIdleTimeMillis(50);
    config.setNumTestsPerEvictionRun(1);

    AbandonedConfig immediateAbandonedConfig = new AbandonedConfig();
    immediateAbandonedConfig.setRemoveAbandonedOnBorrow(true);
    immediateAbandonedConfig.setRemoveAbandonedOnMaintenance(true);
    immediateAbandonedConfig.setLogAbandoned(true);
    immediateAbandonedConfig.setLogWriter(new PrintWriter(System.out));
    // The abandoned timeout is critical. abandoned objects will be removed from pool if it's been abandoned for n seconds.
    immediateAbandonedConfig.setRemoveAbandonedTimeout(0); // 0 is seconds.

    AbandonedConfig longAbandonedConfig = new AbandonedConfig();
    longAbandonedConfig.setRemoveAbandonedOnBorrow(true);
    longAbandonedConfig.setRemoveAbandonedOnMaintenance(true);
    longAbandonedConfig.setLogAbandoned(true);
    longAbandonedConfig.setLogWriter(new PrintWriter(System.out));
    longAbandonedConfig.setRemoveAbandonedTimeout(5);

    goodPool = new GenericObjectPool<>(new IntegerFactory(), config);
    recoverablePool = new GenericObjectPool<>(new IntegerFactory(), config, immediateAbandonedConfig);
    nonRecoverablePool = new GenericObjectPool<>(new IntegerFactory(), config, longAbandonedConfig);
  }

  @Test
  public void testBorrowAndReturn() throws Exception {
    IntStream.range(0, 100)
      .forEach(n -> {
        Integer x = null;
        try {
          x = goodPool.borrowObject();
        } catch (Exception e) {
          fail("error getting object from pool");
        } finally {
          goodPool.returnObject(x);
        }
      });

    assertEquals(100, goodPool.getBorrowedCount());
    assertEquals(100, goodPool.getReturnedCount());

    // everything works well if the objects are returned.
    goodPool.borrowObject();
  }

  @Test
  public void testBorrowAndNoReturn() throws Exception {
    IntStream.range(0, 5)
      .forEach(n -> {
        try {
          Integer x = goodPool.borrowObject();
        } catch (Exception e) {
          fail("error getting object from pool");
        }
      });
    assertEquals(5, goodPool.getBorrowedCount());
    assertEquals(0, goodPool.getReturnedCount());

    thrown.expect(NoSuchElementException.class);
    // this is expected to fail since objects are not returned.
    goodPool.borrowObject();
  }

  @Test
  public void testAbandonConfig() throws Exception {
    IntStream.range(0, 5)
      .forEach(n -> {
        try {
          recoverablePool.borrowObject();
        } catch (Exception e) {
          fail("error getting object from pool");
        }
      });
    assertEquals(5, recoverablePool.getBorrowedCount());
    assertEquals(0, recoverablePool.getReturnedCount());

    Thread.sleep(200);
    // The below will succeed since the borrowed objects will be considered abandoned.
    recoverablePool.borrowObject();
  }

  @Test
  public void testNonRecoverableAbandonConfig() throws Exception {
    IntStream.range(0, 5)
      .forEach(n -> {
        try {
          nonRecoverablePool.borrowObject();
        } catch (Exception e) {
          fail("error getting object from pool");
        }
      });
    assertEquals(5, nonRecoverablePool.getBorrowedCount());
    assertEquals(0, nonRecoverablePool.getReturnedCount());

    Thread.sleep(200);
    thrown.expect(NoSuchElementException.class);
    // The below will fail since the abandoned threshold is not arrived yet.
    nonRecoverablePool.borrowObject();
  }
}