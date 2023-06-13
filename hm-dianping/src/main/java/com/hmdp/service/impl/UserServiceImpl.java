package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
//ServiceImpl<UserMapper, User>标准了mapper和实体类
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

	@Resource
	private StringRedisTemplate stringRedisTemplate;
	@Override
	public Result sendCode(String phone, HttpSession session) {
		//1.校验手机号，检验三方满足正则表达式
		if (RegexUtils.isPhoneInvalid(phone)){
			//2.如果不符合，返回错误信息
			return Result.fail("手机号格式错误");
		}
		//3.符和，则生成验证码,用随机生成器andomUtil随机生成
		String code =RandomUtil.randomNumbers(6);
		//4.保存验证码到redis
		stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
		//5.发送验证码
	    System.out.println("验证码："+code);
		//6.返回ok
		return Result.ok();
	}

	@Override
	public Result login(LoginFormDTO loginForm, HttpSession session) {
		//1.校验手机号
		String phone=loginForm.getPhone();
		if (RegexUtils.isPhoneInvalid(phone)){
			//2.如果不符合，返回错误信息
			return Result.fail("手机号格式错误");
		}
		//3.校验验证码,从redis中提取出刚刚获取的验证码
		String cachecode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone);
		String code =loginForm.getCode();
		//验证码可能过期了，所以要进行判空操作
		if (cachecode==null||!cachecode.toString().equals(code)){
			//3.不一致
			return Result.fail("验证码错误");
		}
		//4.一致，根据手机号查询用户select * from tb_user where phone=?
         User user=query().eq("phone",phone).one();
		//5.判断用户是否存在
		if (user==null){
			//6.不存在，创建新用户并且保存
			user = craeteUserWithPhone(phone);
			
		}
		//7.保存用户信息到redis
		//7.1生成一个token，作为登录令牌
		String token=UUID.randomUUID().toString(true);
		//7.2将User对象转为hash存储
		UserDTO userDTO=BeanUtil.copyProperties(user,UserDTO.class);
		Map<String,Object>userMap=BeanUtil.beanToMap(userDTO,new HashMap<>(),
				CopyOptions.create()
						.setIgnoreNullValue(true)
						.setFieldValueEditor((fieldName,fieldValue) -> fieldValue.toString()));
		//7.3存储
		String tokenKey=LOGIN_USER_KEY+token;
		stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
		//7.4设置有效期
		stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL,TimeUnit.MINUTES);
		return Result.ok(token);
	}

	private User craeteUserWithPhone(String phone) {
		//1.创建用户
		User user= new User();
		user.setPhone(phone);
		//随机生成一个ni称
		user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));
        //2.保存用户
		save(user);
		return user;
	}
}
