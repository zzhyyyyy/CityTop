package com.CityTop.service;

import com.CityTop.dto.Result;
import com.CityTop.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {

    Boolean isFollow(Long followId);

    Result follow(Long followId, boolean status);

    Result common(Long id);
}
