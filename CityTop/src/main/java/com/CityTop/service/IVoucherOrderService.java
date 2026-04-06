package com.CityTop.service;

import com.CityTop.dto.Result;
import com.CityTop.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result purchase(Long voucherId);

    void createVoucherOrder(VoucherOrder voucherOrder);
}
