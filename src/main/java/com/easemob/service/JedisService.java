package com.easemob.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

@Service
public class JedisService {

    @Autowired
    protected StringRedisTemplate jedis;
    @Autowired
    protected Gson gson;

    protected double jedisDouble(String key) {
        return gson.fromJson(jedis.boundHashOps(key).get(key).toString(), double.class);
    }
}
