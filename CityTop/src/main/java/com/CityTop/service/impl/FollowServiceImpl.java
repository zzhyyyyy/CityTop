package com.CityTop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.CityTop.dto.Result;
import com.CityTop.entity.Follow;
import com.CityTop.entity.User;
import com.CityTop.mapper.FollowMapper;
import com.CityTop.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.CityTop.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
    @Autowired
    private UserServiceImpl userService;
    @Override
    public Boolean isFollow(Long followId) {
        List<Follow> l = list(new QueryWrapper<Follow>().eq("follow_user_id", followId).eq("user_id", UserHolder.getUser().getId()));
        return !l.isEmpty();
    }

    @Override
    public Result follow(Long followId, boolean status) {
        if(status){
            boolean success = save(new Follow().setUserId(UserHolder.getUser().getId()).setFollowUserId(followId));
            return success ? Result.ok() : Result.fail("关注失败");
        }
        else {
            boolean success = remove(new QueryWrapper<Follow>().eq("user_id", UserHolder.getUser().getId()).eq("follow_user_id", followId));
            return success ? Result.ok() : Result.fail("取消关注失败");
        }
    }

    @Override
    public Result common(Long id) {
        Long userId = UserHolder.getUser().getId();
        List<Long> list1 = list(new QueryWrapper<Follow>()
                .eq("user_id", userId))
                .stream()
                .map(Follow::getFollowUserId)
                .collect(Collectors.toList());  // ← 改用这个
        List<Long> list2 = list(new QueryWrapper<Follow>()
                .eq("user_id", id))
                .stream()
                .map(Follow::getFollowUserId)
                .collect(Collectors.toList());
        list1.retainAll(list2);
        List<User> ans = new ArrayList<>();
        for (Long followUserId : list1) {
            User user = userService.getById(followUserId);
            if (user != null) {
                ans.add(user);
            }
        }
        return Result.ok(ans);
    }
}
