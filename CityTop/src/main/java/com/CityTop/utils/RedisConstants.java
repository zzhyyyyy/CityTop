package com.CityTop.utils;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;

    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";

    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String FEED_KEY = "feed:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";

    // ===== 短信验证码接口限流 =====
    public static final String RATE_LIMIT_SMS_IP_KEY = "rate:sms:ip:";      // 同IP限流key前缀
    public static final String RATE_LIMIT_SMS_PHONE_KEY = "rate:sms:phone:"; // 同手机号限流key前缀
    public static final int RATE_LIMIT_SMS_IP_MAX = 10;     // 同IP每窗口最多次数
    public static final int RATE_LIMIT_SMS_PHONE_MAX = 1;   // 同手机号每窗口最多次数
    public static final int RATE_LIMIT_SMS_WINDOW = 60;     // 窗口大小（秒）
}
