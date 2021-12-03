package com.nowcoder.community.service;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class FollowService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    // 关注
    public void follow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                redisOperations.multi();

                redisTemplate.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                redisTemplate.opsForZSet().add(followerKey, userId, System.currentTimeMillis());
                return redisOperations.exec();
            }
        });
    }

    // 取消关注
    public void unfollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                redisOperations.multi();

                redisTemplate.opsForZSet().remove(followeeKey, entityId);
                redisTemplate.opsForZSet().remove(followerKey, userId);
                return redisOperations.exec();
            }
        });
    }

    // 获取某人关注目标实体数
    public long getFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().size(followeeKey);
    }

    // 获取某人粉丝数
    public long getFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().size(followerKey);
    }

    // 获取关注状态
    public int getFollowStatus(int userId, int entityId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
//        //按照排名先后(从小到大)打印指定区间内的元素, -1为打印全部
//        Set<String> range = redisTemplate.opsForZSet().range(followeeKey, 0, -1);
//        return range.contains(entityId) ? 1 : 0;
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null ? 1 : 0;
    }

    // 获取用户关注的人
    public List<User> getFollowees(int userId, int entityType, int offset, int limit) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);

        // 按时间排名从大到小排序
        Set<Integer> ids = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit -1);

        if (ids == null) {
            return null;
        }

        List<User> users = new ArrayList<>();
        for (Integer id : ids){
            User user = userService.findUserById(id);
            users.add(user);
        }
        return users;
    }

    // 获取关注某用户的分数（关注时间）
    public double getFolloweeTime(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);

        return redisTemplate.opsForZSet().score(followeeKey, entityId);
    }

    // 获取实体的粉丝
    public List<User> getFollowers(int entityId, int entityType, int offset, int limit) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

        // 按时间排名从大到小排序，-1表示获得全部
//        Set<Integer> ids = redisTemplate.opsForZSet().reverseRange(followerKey, 0, -1);

        Set<Integer> ids = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);

        if (ids == null) {
            return null;
        }

        List<User> users = new ArrayList<>();
        for (Integer id : ids){
            User user = userService.findUserById(id);
            users.add(user);
        }
        return users;
    }

    // 获取被粉丝关注的时间
    public double getFollowerTime(int userId, int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

        return redisTemplate.opsForZSet().score(followerKey, userId);
    }
}
