package com.CityTop.service;

/**
 * 通过当前启用的通知通道向用户投递领域事件。
 */
public interface NotificationService {

    void notifyUser(Long userId, String type, String content);
}
