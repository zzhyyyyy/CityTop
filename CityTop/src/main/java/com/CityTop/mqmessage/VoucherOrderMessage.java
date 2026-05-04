package com.CityTop.mqmessage;

import com.CityTop.entity.VoucherOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherOrderMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long voucherId;
    private Long userId;
    private Long orderId;
    private LocalDateTime createTime;
    
    // 重试次数（用于消费者失败重试）
    private int retryCount;
    
    // 转换为实体对象
    public VoucherOrder toVoucherOrder() {
        VoucherOrder order = new VoucherOrder();
        order.setVoucherId(voucherId);
        order.setUserId(userId);
        order.setId(orderId);
        order.setCreateTime(createTime);
        return order;
    }
}
