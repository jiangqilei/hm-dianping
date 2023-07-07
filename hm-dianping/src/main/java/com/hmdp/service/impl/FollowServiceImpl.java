package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
	@Resource
	private StringRedisTemplate stringRedisTemplate;
	@Resource
	private UserServiceImpl userService;
	@Override
	public Result follow(Long followUserId, Boolean isFollow) {
		//1.获取用户信息
		Long userId = UserHolder.getUser().getId();
		String key = "follows:" + userId;
		//2.判断是关注还是取关
		if (isFollow){
			//关注，保存用户与关注对象的关联关系
			Follow follow=new Follow();
			follow.setUserId(userId);
			follow.setFollowUserId(followUserId);
			boolean isSuccess = save(follow);
			if (isSuccess){
				//把关注用户的id，放入redis中的set集合 sadd userId followerUserId
				stringRedisTemplate.opsForSet().add(key,followUserId.toString());
			}
		}else {
			//取关，删除对应的数据
			QueryWrapper<Follow> queryWrapper=new QueryWrapper<>();
			queryWrapper.eq("user_id", userId);
			queryWrapper.eq("follow_user_id", followUserId);
			boolean isSuccess = remove(queryWrapper);
			if (isSuccess){
				// 把关注用户的id从Redis集合中移除
				stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
			}
		}
		return Result.ok();
	}

	@Override
		public Result isFollow(Long followUserId) {
			//1.获取登录用户
			Long userId = UserHolder.getUser().getId();
			//2.查询是否关注 我们只需要查询出是否存在数据而不需要具体的数据
			Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
			//3.判断count是否大于0
			return Result.ok(count>0);
		}

	@Override
	public Result followCommons(Long id) {
		// 1.获取当前用户
		Long userId = UserHolder.getUser().getId();
		String key = "follows:" + userId;
		// 2.求交集
		String key2 = "follows:" + id;
		Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
		if (intersect == null || intersect.isEmpty()) {
			// 无交集
			return Result.ok(Collections.emptyList());
		}
		// 3.解析id集合
		//这段代码的目的是将交集集合中的字符串元素转换为 Long 类型，
		// 并以 List<Long> 的形式保存起来。
		List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
		// 4.查询用户
		//这段代码的目的是查询指定用户ID列表的用户信息，并将查询结果转换为一个包含 UserDTO 对象的 List
		List<UserDTO> users = userService.listByIds(ids)
				.stream()
				.map(user -> BeanUtil.copyProperties(user, UserDTO.class))
				.collect(Collectors.toList());
		return Result.ok(users);
	}


}
