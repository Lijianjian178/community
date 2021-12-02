package com.nowcoder.community.service;

import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LikeService implements CommunityConstant {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    // 点赞
    public void like(int userId, int entityType, int entityId, int targetId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        String userLikeKey = RedisKeyUtil.getUserLikeKey(targetId);
        boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
        if (isMember) {
            redisTemplate.opsForSet().remove(entityLikeKey, userId);
            // 取消点赞后减少作者点赞数
            redisTemplate.opsForValue().decrement(userLikeKey);
        } else {
            redisTemplate.opsForSet().add(entityLikeKey, userId);
            // 增加作者被点赞数
            redisTemplate.opsForValue().increment(userLikeKey);
        }
    }

    // 查询实体点赞的数量
    public long findEntityLikeCount(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    // 查询某人对某实体的点赞状态
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }

    // 获得某用户被点赞数
    public long findUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        // 如果用户未收到过赞，设为0
        redisTemplate.opsForValue().setIfAbsent(userLikeKey, 0);
        return Long.parseLong(redisTemplate.opsForValue().get(userLikeKey).toString());
    }
}
