package com.CityTop.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.CityTop.dto.LoginFormDTO;
import com.CityTop.dto.Result;
import com.CityTop.dto.UserDTO;
import com.CityTop.entity.User;
import com.CityTop.entity.UserInfo;
import com.CityTop.service.IUserInfoService;
import com.CityTop.service.IUserService;
import com.CityTop.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static com.CityTop.utils.RedisConstants.LOGIN_USER_KEY;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }

    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session) {
        String cacheCode = redisTemplate.opsForValue().get("user:code:" + loginForm.getPhone());
        log.info("login verify code={}", cacheCode);
        if (cacheCode == null || !cacheCode.equals(loginForm.getCode())) {
            return Result.fail("Verification code error");
        }

        User user = userService.getOne(new QueryWrapper<User>().eq("phone", loginForm.getPhone()));
        if (user == null) {
            user = new User();
            user.setPhone(loginForm.getPhone());
            user.setNickName("user_" + RandomUtil.randomString(10));
            userService.save(user);
            log.info("user register success: {}", user);
        }

        String token = UUID.fastUUID().toString(true);
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", userDTO.getId().toString());
        userMap.put("nickName", userDTO.getNickName());
        userMap.put("icon", userDTO.getIcon());
        redisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, userMap);

        return Result.ok(token);
    }

    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (token == null || token.trim().isEmpty()) {
            return Result.ok();
        }
        redisTemplate.delete(LOGIN_USER_KEY + token);
        UserHolder.removeUser();
        return Result.ok();
    }

    @GetMapping("/me")
    public Result me() {
        UserDTO user = UserHolder.getUser();
        log.info("current user={}", user);
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId) {
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        return Result.ok(info);
    }

    @GetMapping("/{id}")
    public Result getOtherInfoById(@PathVariable("id") Long userId) {
        return userService.getOtherInfoById(userId);
    }

    @PostMapping("/sign")
    public Result sign() {
        return userService.sign();
    }

    @GetMapping("/sign/count")
    public Result signCount() {
        return userService.signCount();
    }
}
