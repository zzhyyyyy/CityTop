package com.CityTop.service.impl;

import com.CityTop.dto.Result;
import com.CityTop.dto.UserDTO;
import com.CityTop.entity.Blog;
import com.CityTop.entity.User;
import com.CityTop.mapper.BlogMapper;
import com.CityTop.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.CityTop.service.IUserService;
import com.CityTop.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Autowired
    private IUserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IBlogService iBlogService;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
        blog.setIsLike(isLike(id));
        return Result.ok(blog);
    }

    @Override
    public Result likeBlog(Long id) {
        Blog blog = getById(id);
        if(isLike(id))
        {
            boolean success = iBlogService.update()
                    .setSql("liked = liked - 1").eq("id", id).update();
            if(success) {
                stringRedisTemplate.opsForZSet().remove("blog:liked:" + id, UserHolder.getUser().getId().toString());
                blog.setIsLike(false);
            }
        }
        else {
            boolean success = iBlogService.update()
                    .setSql("liked = liked + 1").eq("id", id).update();
            if (success) {
                stringRedisTemplate.opsForZSet().add("blog:liked:" + id, UserHolder.getUser().getId().toString(), System.currentTimeMillis());
                blog.setIsLike(true);
            }
        }
        return Result.ok(blog);
    }

    public Boolean isLike(Long id) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        Blog blog = getById(id);
        // 判断当前登录用户是否已经点赞
        String key = "blog:liked:" + id;
        boolean Liked = stringRedisTemplate.opsForZSet().score(key, user.getId().toString()) != null;
        blog.setIsLike(Liked);
        return Liked;
    }

    @Override
    public Result queryBlogLikes(Long id) {
        Set<String> range = stringRedisTemplate.opsForZSet().range("blog:liked:" + id, 0, 4);
        List<User> l = new ArrayList<>();
        if (range != null) {
            for (String s : range) {
                User user = userService.getById(Long.valueOf(s));
                l.add(user);
            }
        }
        return Result.ok(l);
    }
}
