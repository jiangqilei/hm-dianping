package com.hmdp.utils;

import cn.hutool.core.collection.CollectionUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.UUID;
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
	private static final String ID_PREFIX= UUID.randomUUID().toString() +"-";
	//初始化lua脚本
	private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
	static {
		UNLOCK_SCRIPT=new DefaultRedisScript<>();
		UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
	}
	@Override
	public boolean tryLock(long timeoutSec) {
		//获取线程提示
		String threadid = ID_PREFIX+Thread.currentThread().getId();
		//获取锁
		Boolean success= stringRedisTemplate.opsForValue()
				.setIfAbsent(KEY_PREFIX+name,threadid+"",timeoutSec, TimeUnit.SECONDS);
		//使用.equals()方法来比较该Boolean对象是否与Boolean.TRUE相等。
		return Boolean.TRUE.equals(success);
	}

//	@Override
//	public void unlock() {
//		//获取线程标识
//		String threadid = ID_PREFIX+Thread.currentThread().getId();
//		//获取锁中的标识
//		String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//		//判断标识是否一致
//		if (threadid.equals(id)){
//			//释放锁
//			stringRedisTemplate.delete(KEY_PREFIX+name);
//		}
//
//	}
	@Override
	public void unlock() {
		//调用lua脚本
		stringRedisTemplate.execute(
				UNLOCK_SCRIPT, Collections.singletonList(KEY_PREFIX+name), ID_PREFIX+Thread.currentThread().getId());
	}
}
