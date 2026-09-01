package com.CityTop.service;

/**
 * Delivers a user-facing domain event through the active notification channel.
 */
public interface NotificationService {

    void notifyUser(Long userId, String type, String content);
}
