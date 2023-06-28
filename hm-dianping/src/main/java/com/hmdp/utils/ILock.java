package com.hmdp.utils;

public interface ILock {
/**
 ★尝试获取锁
 *@param timeoutsec锁持有的超时时间，过期后自动释放*@return true代表获取锁成功; false代表获取锁失败★/
*/
 public boolean tryLock(long timeoutSec);
 public void unlock();
}
