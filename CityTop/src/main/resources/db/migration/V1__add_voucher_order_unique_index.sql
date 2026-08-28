-- 生产库修复脚本：执行前先处理已有的 (user_id, voucher_id) 重复订单。
-- 该索引是应用层防重复逻辑的最终数据一致性保障。
ALTER TABLE `tb_voucher_order`
    ADD UNIQUE INDEX `uk_user_voucher` (`user_id`, `voucher_id`);
