package com.wandoujia.flashbot;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xudong on 2/7/14.
 */
public class IdGenerator {
  private static AtomicInteger index = new AtomicInteger();

  public static int next() {
    return index.incrementAndGet();
  }
}
