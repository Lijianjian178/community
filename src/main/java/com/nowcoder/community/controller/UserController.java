package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private DiscussPostService discussPostService;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage() {
        return "/site/setting";
    }

    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null){
            model.addAttribute("error", "您还没有选择图片！");
            return "/site/setting";
        }

        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)){
            model.addAttribute("error", "文件格式不正确！");
            return "/site/setting";
        }

        // 生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;
        // 确定文件存放的路径
        File dest = new File(uploadPath + "/" + fileName);
        try {
            // 存储文件
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败：" + e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常！" + e);
        }

        // 更新当前用户的头像的路径(web访问路径)
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }

    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 服务器存放路径
        fileName = uploadPath + "/" + fileName;
        // 文件的后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // 响应图片
        response.setContentType("image/" + suffix);
        try (
                OutputStream os = response.getOutputStream();
                FileInputStream fis = new FileInputStream(fileName);
                ){
            byte[] buffer = new byte[1024];
            int b = 0;
            while((b = fis.read(buffer)) != -1){
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败：" + e.getMessage());
        }
    }

    @LoginRequired
    @RequestMapping(path = "/updatePassword", method = RequestMethod.POST)
    public String updatePassword(String oldPassword, String newPassword, Model model) {
        User user = hostHolder.getUser();
        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        if (!oldPassword.equals(user.getPassword())){
            model.addAttribute("passwordMsg", "原始密码不正确！");
            return "/site/setting";
        }

        newPassword = CommunityUtil.md5(newPassword + user.getSalt());
        userService.updatePassword(user.getId(), newPassword);
        return "redirect:/index";
    }

    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(Model model, @PathVariable("userId") int userId) {
        // 打开用户页面
        User user = userService.findUserById(userId);
        model.addAttribute("user", user);

        // 登录用户
        User loginUser = hostHolder.getUser();
        model.addAttribute("loginUser", loginUser);

        // 用户收到点赞数
        model.addAttribute("userLikeCount", likeService.findUserLikeCount(user.getId()));

        // 关注目标数
        model.addAttribute("followeeCount", followService.getFolloweeCount(userId, ENTITY_TYPE_USER));
        // 粉丝数
        model.addAttribute("followerCount", followService.getFollowerCount(ENTITY_TYPE_USER, userId));
        // 关注状态
        if (hostHolder.getUser() != null) {
            model.addAttribute("followStatus", followService.getFollowStatus(loginUser.getId(), userId, ENTITY_TYPE_USER));
        }

        return "/site/profile";
    }

    @RequestMapping(path = "/{userId}/{entityType}/followee", method = RequestMethod.GET)
    public String getFollowees(Model model, @PathVariable("userId") int userId, Page page, @PathVariable("entityType") int entityType) {
        User user = userService.findUserById(userId);
        if(user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user", user);

        page.setRows((int) followService.getFolloweeCount(userId, entityType));
        page.setLimit(5);
        page.setPath("/user/" + userId + "/" + entityType + "/followee");

        List<User> userSet = followService.getFollowees(userId, entityType, page.getOffset(), page.getLimit());
        List<Map<String, Object>> users = new ArrayList<>();
        if (userSet != null) {
            for (User user1 : userSet) {
                Map<String, Object> map = new HashMap<>();
                // 关注对象用户
                map.put("user", user1);
                // 关注时间
                String followTime =
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(followService.getFolloweeTime(userId, entityType, user1.getId()));
                map.put("followTime", followTime);
                users.add(map);
            }
        }

        model.addAttribute("users", users);

        return "/site/followee";
    }

    @RequestMapping(path = "/{userId}/{entityType}/follower", method = RequestMethod.GET)
    public String getFollowers(Model model, @PathVariable("userId") int userId, @PathVariable("entityType") int entityType, Page page) {
        User user = userService.findUserById(userId);
        model.addAttribute("user", user);

        User loginUser = hostHolder.getUser();
        model.addAttribute("loginUser", loginUser);

        page.setRows((int) followService.getFollowerCount(entityType, userId));
        page.setLimit(5);
        page.setPath("/user/" + userId + "/" + entityType + "/follower");

        List<User> userList = followService.getFollowers(userId, entityType, page.getOffset(), page.getLimit());
        List<Map<String, Object>> users = new ArrayList<>();
        if (userList != null) {
            for (User user1 : userList) {
                Map<String, Object> map = new HashMap<>();
                // 粉丝
                map.put("user", user1);
                // 受关注时间
                String followTime =
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(followService.getFollowerTime(user1.getId(), entityType, userId));
                map.put("followTime", followTime);
                // 关注状态
                map.put("followStatus", followService.getFollowStatus(userId, user1.getId(), ENTITY_TYPE_USER));
                users.add(map);
            }
        }

        model.addAttribute("users", users);
        return "/site/follower";
    }

    @RequestMapping(path = "/my-post/{userId}", method = RequestMethod.GET)
    public String getMyPostPage(Model model, @PathVariable("userId") int userId, Page page) {

        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user", user);
        // 贴子数
        int count = discussPostService.findDiscussPostRows(userId);
        model.addAttribute("count", count);
        // 分页
        page.setRows(count);
        page.setPath("/my-post/" + userId);
        page.setLimit(5);
        // 帖子列表
        List<DiscussPost> posts = discussPostService.findDiscussPosts(userId, page.getOffset(), page.getLimit(), 0);
        List<Map<String, Object>> postVO = new ArrayList<>();
        if (posts != null) {
            for (DiscussPost post : posts) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);

                long likeCount = likeService.findEntityLikeCount(CommunityConstant.ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);
                postVO.add(map);
            }
        }

        model.addAttribute("posts", postVO);

        return "/site/my-post";
    }
}
