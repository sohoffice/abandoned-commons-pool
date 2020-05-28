package sohoffice.example.abandonedpool;

import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class IntegerFactory extends BasePooledObjectFactory<Integer> {

  private static final AtomicInteger curInteger = new AtomicInteger(1);

  @Override
  public Integer create() throws Exception {
    return curInteger.getAndAdd(1);
  }

  @Override
  public PooledObject<Integer> wrap(Integer n) {
    return new DefaultPooledObject<>(n);
  }

}
