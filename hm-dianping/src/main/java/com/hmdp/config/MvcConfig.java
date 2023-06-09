package com.hmdp.config;

import com.hmdp.utils.RefreshTokenUBterceptor;
import com.hmdp.utils.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
	@Resource
	private StringRedisTemplate stringRedisTemplate;
	//添加拦截器
	@Override
	public void addInterceptors(InterceptorRegistry registry){
		registry.addInterceptor(new LoginInterceptor())
				//排除一些不需要被拦截的,比如一些跟登入用户无关的就不需要被拦截
				.excludePathPatterns(


						"/shop/**",
						"/shop-type/**",
						"/upload/**",
						"/voucher/**",
						"/blog/hot",
						"/user/code",
						"/user/login"
//						"/user/me"
				).order(1);
		registry.addInterceptor(new RefreshTokenUBterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0);
	}
}
