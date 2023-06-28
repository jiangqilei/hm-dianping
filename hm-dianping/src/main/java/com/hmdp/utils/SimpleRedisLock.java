package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{
//	@Resource
	private String name;
	private StringRedisTemplate stringRedisTemplate;
    public SimpleRedisLock(String name,StringRedisTemplate stringRedisTemplate){
		this.name=name;
		this.stringRedisTemplate=stringRedisTemplate;
	}
	private static final String KEY_PREFIX="lock";
	@Override
	public boolean tryLock(long timeoutSec) {
		//获取线程提示
		long threadid = Thread.currentThread().getId();
		//获取锁
		Boolean success= stringRedisTemplate.opsForValue()
				.setIfAbsent(KEY_PREFIX+name,threadid+"",timeoutSec, TimeUnit.SECONDS);
		//使用.equals()方法来比较该Boolean对象是否与Boolean.TRUE相等。
		return Boolean.TRUE.equals(success);
	}

	@Override
	public void unlock() {
		//释放锁
		stringRedisTemplate.delete(KEY_PREFIX+name);
	}
}
