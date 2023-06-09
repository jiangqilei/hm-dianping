package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RefreshTokenUBterceptor implements HandlerInterceptor {
	private StringRedisTemplate stringRedisTemplate;
	public RefreshTokenUBterceptor(StringRedisTemplate stringRedisTemplate){
		this.stringRedisTemplate=stringRedisTemplate;
	}
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		//1.获取token,token是在请求头里
		String token=request.getHeader("Authorization");
		if (StrUtil.isBlank(token)){
			//不存在，拦截，返回401状态
//			response.setStatus(401);
			return true;
		}
		//2.以token为key获取redis中的用户
		String key=RedisConstants.LOGIN_USER_KEY+token;
		Map<Object,Object> userMap=stringRedisTemplate.opsForHash().entries(key);
		//3.判断用户是否存在
		if (userMap.isEmpty()) {
			//4.不存在拦截 返回状态401
//			response.setStatus(401);
			return true;
		}
		//5.将查询到的hash数据转为UserDTO对象
		UserDTO userDTO= BeanUtil.fillBeanWithMap(userMap,new UserDTO(),false);
		//6.存在，保存用户信息到ThreadLocal
		UserHolder.saveUser(userDTO);
		//刷新token有效期
		stringRedisTemplate.expire(key,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
		//7.放行
		return true;
	}
}
