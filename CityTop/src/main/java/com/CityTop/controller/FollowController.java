package com.CityTop.controller;


import com.CityTop.dto.Result;
import com.CityTop.service.IFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Autowired
    private IFollowService followService;
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followId) {
        return Result.ok(followService.isFollow(followId));
    }
    @PutMapping("/{id}/{status}")
    public Result follow(@PathVariable("id") Long followId, @PathVariable Boolean status) {
        return Result.ok(followService.follow(followId, status));
    }

    @GetMapping("/common/{id}")
    public Result common(@PathVariable("id") Long id) {
        return followService.common(id);
    }

}
