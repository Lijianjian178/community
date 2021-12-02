package com.nowcoder.community.controller;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FollowController {

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityId, int entityType, int followFlag) {
        User user = hostHolder.getUser();
        System.out.println(entityId + entityType + followFlag);

        // 关注,followFlag,0表示关注，1表示取消关注
        if (followFlag == 0) {
            followService.follow(user.getId(), entityType, entityId);
            return CommunityUtil.getJSONString(0, "关注成功！");
        } else {
            // 取消关注
            followService.unfollow(user.getId(), entityType, entityId);
            return CommunityUtil.getJSONString(0, "取消成功！");
        }
    }
}
