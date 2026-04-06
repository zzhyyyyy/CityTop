package com.CityTop.service;

import com.CityTop.dto.Result;
import com.CityTop.entity.Blog;
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

    Result queryBlogById(Long id);

    Result likeBlog(Long id);

    Boolean isLike(Long id);

    Result queryBlogLikes(Long id);
}
