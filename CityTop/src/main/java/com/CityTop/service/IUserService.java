package com.CityTop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.CityTop.dto.Result;
import com.CityTop.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result getOtherInfoById(Long userId);

    Result sign();

    Result signCount();
}
