package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
	private StringRedisTemplate stringRedisTemplate;
    @Resource
	private CacheClient cacheClient;

	//缓存穿透的业务
	@Override
	public Result queryByid(Long id) {
		//用工具类来实现缓存穿透的业务
        //Shop shop =cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id, Shop.class, this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);
		//互斥锁解决缓存击穿
		//Shop shop= queryWithMutex(id);
		//用工具类来实现基于逻辑的缓存击穿问题
		Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY,id, Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);
		if (shop==null){
			return Result.fail("店铺不存在!");
		}
		return Result.ok(shop);
	}




	//缓存击穿————互斥锁
	public Shop queryWithMutex(Long id){
		String key=CACHE_SHOP_KEY+id;
		//1.从redis查询商铺缓存
		String shopJson = stringRedisTemplate.opsForValue().get(key);
		// 2.1判断是否存在
		if (StrUtil.isNotBlank(shopJson)){
			//3.存在，直接返回
			Shop shop= JSONUtil.toBean(shopJson,Shop.class);
			return shop;
		}
		//2.2判断命中的是否是空值
		if(shopJson != null){
			//返回一个错误信息
			return null;
		}
		//4.实现缓存重建
		String lockKey=LOCK_SHOP_KEY+id;
		Shop shop = null;
		try {
			//4.1.获取互斥锁
			boolean isLock=trylock(lockKey);
			//4.2.判断是否获取成功
			if(!isLock){
				//4.3.失败，则休眠并重试
				Thread.sleep(50);
				//递归
				return queryWithMutex(id);
			}
			//4.4如果成功，根据id查询数据库
			 shop = getById(id);
			//模拟重建延时
			Thread.sleep(200);
			//5.不存在，返回错误
			if(shop==null){
				//将空值写入redis，来解决缓存穿透
				stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
				return null;
			}
			//6.存在，写入redis
			stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);

		}catch (InterruptedException e){
			throw new RuntimeException(e);
		}finally {
			//7.释放互斥锁
			unlock(lockKey);
		}
		return shop;
	}



	//缓存穿透的业务
	public Shop queryWithPassThrough(Long id){
		String key=CACHE_SHOP_KEY+id;
		//1.从redis查询商铺缓存
		String shopJson = stringRedisTemplate.opsForValue().get(key);
		// 2.1判断是否存在
		if (StrUtil.isNotBlank(shopJson)){
			//3.存在，直接返回
			Shop shop= JSONUtil.toBean(shopJson,Shop.class);
			return shop;
		}
		//2.2判断命中的是否是空值
		if(shopJson != null){
			//返回一个错误信息
			return null;
		}
		//4.不存在，根据id查询数据库
		Shop shop = getById(id);
		//5.不存在，返回错误
		if(shop==null){
			//将空值写入redis，来解决缓存穿透
			stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
			return null;
		}
		//6.存在，写入redis
		stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
		//7.返回

		return shop;
	}



	//尝试获取锁
    private  boolean trylock(String key){
		Boolean flag =stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.MINUTES);
		return BooleanUtil.isTrue(flag);
	}


	//释放锁
	private void unlock(String key){
		stringRedisTemplate.delete(key);
	}



	@Override
	public Result update(Shop shop) {
		Long id = shop.getId();
		if (id == null){
			return Result.fail("店铺id不能为空");
		}
          //1.更新数据库
		  updateById(shop);
		  //2.删除缓存
          stringRedisTemplate.delete(CACHE_SHOP_KEY+shop.getId());

        return Result.ok();
	}
}
