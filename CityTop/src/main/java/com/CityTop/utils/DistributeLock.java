package com.CityTop.utils;

public interface DistributeLock {
    boolean tryLock(long timeoutSec);
    void unlock();
}
