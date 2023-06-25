package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.tokens.Token;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {
	private final StringRedisTemplate stringRedisTemplate;
	public CacheClient(StringRedisTemplate stringRedisTemplate){
		this.stringRedisTemplate = stringRedisTemplate;
	}
	public void set(String key, Object value, Long time, TimeUnit unit){
		stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
	}

	//设置逻辑过期时间
	public void setWithLogicalExpire(String key,Object value,Long time,TimeUnit unit){
		//设置逻辑过期时间
		RedisData redisData=new RedisData();
		redisData.setData(value);
		//LocalDateTime.now()返回当前的本地日期和时间。
		// plusSeconds()方法用于在当前时间的基础上增加指定的秒数。
		redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
		//写入Redis
		stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
	}

	//用工具类缓存穿透
	public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback, Long time, TimeUnit unit){
		String key=keyPrefix+id;
		//1.从redis查询商铺缓存
		String Json = stringRedisTemplate.opsForValue().get(key);
		// 2.1判断是否存在
		if (StrUtil.isNotBlank(Json)){
			//3.存在，直接返回
			return JSONUtil.toBean(Json,type);
		}
		//2.2判断命中的是否是空值
		if(Json != null){
			//返回一个错误信息
			return null;
		}
		//4.不存在，根据id查询数据库
		R r = dbFallback.apply(id);
		//5.不存在，返回错误
		if(r==null){
			//将空值写入redis，来解决缓存穿透
			stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
			return null;
		}
		//6.存在，写入redis
        this.set(key,r,time,unit);
		//7.返回

		return r;
	}


//    //缓存击穿
//	public <ID,R> R queryWithLogicalExpire(String keyPrefix,ID id,Class<R> type, Function<ID,R> dbFallback,Long time, TimeUnit unit){
//		String key =keyPrefix+id;
//		//1.从redis查询商铺缓存
//		String json = stringRedisTemplate.opsForValue().get(key);
//		//2.判断是否存在
//		if(StrUtil.isBlank(json)){
//			//3.存在，直接返回
//			return null;
//		}
//		//4.命中，需要先把son反序列化为对象
//		RedisData redisData=JSONUtil.toBean(json,RedisData.class);
//		R r=JSONUtil.toBean((JSONObject) redisData.getData(),type);
//		LocalDateTime expireTime = redisData.getExpireTime();
//		//5.判断是否过期
//		if(expireTime.isAfter(LocalDateTime.now())){
//			//5.1未过期，直接返回店铺信息
//			return r;
//		}
//		//5.1已过期，需要缓存重建
//		//6.缓存重建
//		//6.1获取互斥锁
//		String lockKey = LOCK_SHOP_KEY+id;
//		boolean isLock = trylock(lockKey);
//		//6.2判断是否成功获取锁
//		if(isLock){
//			//6.3成功，开启独立线程，实现缓存重建
//			CACHE_REBUILD_EXECUTOR.submit(()->{
//               try{
//				   //查询数据库
//				   dbFallback.apply(id);
//				   //写入redis
//				   this.set(key,r,time,unit);
//
//
//			})
//		}
//	}
//
//	//尝试获取锁
//	private  boolean trylock(String key){
//		Boolean flag =stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.MINUTES);
//		return BooleanUtil.isTrue(flag);
//	}
//
//
//	//释放锁
//	private void unlock(String key){
//		stringRedisTemplate.delete(key);
//	}

}
