package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {
	/**
	 * 查询最热的博客
	 * @param current 前几篇
	 * @return 最热博客
	 */
	Result queryHotBlog(Integer current);
	Result queryBlogById(Long id);
	/**
	 * 点赞博客
	 * @param id 博客id
	 * @return
	 */
	Result likeBlog(Long id);

	Result queryBlogLikes(Long id);
}
