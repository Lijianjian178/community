package com.nowcoder.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
@RequestMapping
public class MessageController implements CommunityConstant {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @RequestMapping(path = "/message/list", method = RequestMethod.GET)
    public String getMessagePage(Model model, Page page) {
        // 获取当前用户
        User user = hostHolder.getUser();

        // 分页信息
        page.setLimit(5);
        page.setPath("/message/list");
        page.setRows(messageService.findConversationCount(user.getId()));
        System.out.println(messageService.findConversationCount(user.getId()));

        // 会话列表
        List<Message> messages = messageService.findLatestMessageByConversationId(user.getId(), page.getOffset(), page.getLimit());
        List<Map<String, Object>> messageVoList = new ArrayList<>();
        if (messages != null) {
            for (Message message : messages) {
                // 信息
                Map<String, Object> map = new HashMap<>();
                map.put("message", message);

                // 信息数量
                map.put("letterCount", messageService.findLetterCount(message.getConversationId()));

                // 未读数量
                int unread = messageService.findUnreadLetterCount(user.getId(), message.getConversationId());
                map.put("unread", unread);

                // 对方用户
                // 分割为111,112，其中不是用户id的另一个id即为对方id
                String[] idArr = message.getConversationId().split("_");
                // 当前用户id
                String userId = String.valueOf(hostHolder.getUser().getId());
                // 对方id
                String counterPartId = userId.equals(idArr[0]) ? idArr[1] : idArr[0];

                User counterPartUser = userService.findUserById(Integer.parseInt(counterPartId));
                map.put("cpUser", counterPartUser);

                messageVoList.add(map);
            }
        }

        model.addAttribute("messages", messageVoList);

        int letterTotalUnread = messageService.findUnreadLetterCount(user.getId(), null);
        model.addAttribute("letterTotalUnread", letterTotalUnread);
        int noticeTotalUnread = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeTotalUnread", noticeTotalUnread);

        return "/site/letter";
    }

    // 得到集合中未读消息的id
    private List<Integer> getLetterIds(List<Message> messages) {
        List<Integer> ids = new ArrayList<>();
        if (messages != null) {
            for (Message message : messages) {
                if (hostHolder.getUser().getId() == message.getToId()) {
                    if (message.getStatus() == 0) {
                        ids.add(message.getId());
                    }
                }
            }
        }

        return ids;
    }

    @RequestMapping(path = "/message/letter-detail/{conversationId}", method = RequestMethod.GET)
    public String getMessageDetailPage(Model model, @PathVariable("conversationId") String conversationId, Page page) {
        // 分页
        page.setLimit(5);
        page.setPath("/message/letter-detail/" + conversationId);
        page.setRows(messageService.findLetterCount(conversationId));

        // 私信列表
        List<Message> letters = messageService.findLettersByConversationId(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letterVoList = new ArrayList<>();
        User cpUser = new User();
        if (letters != null) {
             for (Message letter : letters){
                 Map<String, Object> map = new HashMap<>();
                 // 私信
                 map.put("letter", letter);

                 // 用户
                 if (letter.getFromId() == hostHolder.getUser().getId()) {
                     // 我发的私信
                     map.put("user", hostHolder.getUser());
                 } else {
                     // 对方用户发的私信
                     // 分割为111,112，其中不是用户id的另一个id即为对方id
                     String[] idArr = conversationId.split("_");
                     // 当前用户id
                     String userId = String.valueOf(hostHolder.getUser().getId());
                     // 对方id
                     String counterPartId = userId.equals(idArr[0]) ? idArr[1] : idArr[0];
                     cpUser = userService.findUserById(Integer.parseInt(counterPartId));
                     map.put("user", cpUser);
                 }
                 letterVoList.add(map);
             }
        }

        System.out.println(letterVoList);
        model.addAttribute("cpUser", cpUser);
        model.addAttribute("letterVoList", letterVoList);

        // 设置已读
        List<Integer> ids = getLetterIds(letters);
        if(!ids.isEmpty()) {
            messageService.readMessage(ids);
        }

        return "/site/letter-detail";
    }

    @RequestMapping(path = "/message/send", method = RequestMethod.POST)
    @ResponseBody
    public String addMessage(String toUsername, String content) {
        User user = hostHolder.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(403, "还没有登录！");
        }

        User toUser = userService.findUserByName(toUsername);
        System.out.println(toUser);
        if (toUser == null){
            return CommunityUtil.getJSONString(1, "发送对象不存在！");
        }

        Message message = new Message();
        message.setFromId(user.getId());
        message.setToId(toUser.getId());
        String conversationId = user.getId() >= toUser.getId() ? toUser.getId()+"_"+ user.getId() : user.getId()+"_"+ toUser.getId();
        message.setConversationId(conversationId);
        message.setContent(content);
        message.setCreateTime(new Date());
        message.setStatus(0);
        messageService.addLetter(message);

        return CommunityUtil.getJSONString(0, "发送成功！");
    }

    @RequestMapping(path = "/message/delete_msg/{conversationId}/{id}", method = RequestMethod.GET)
    public String deleteMessage(@PathVariable("id") int id, @PathVariable("conversationId") String conversationId) {
        messageService.deleteMessage(id);
        return "redirect:/message/letter-detail/" + conversationId;
    }

    @RequestMapping(path = "/notice/list", method = RequestMethod.GET)
    public String getNoticeList(Model model) {
        User user = hostHolder.getUser();

        // 查询评论类通知
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);
        Map<String, Object> messageMap = new HashMap<>();
        if (message != null) {
            messageMap.put("message", message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageMap.put("user", userService.findUserById((Integer) data.get("userId")));
            messageMap.put("entityType", data.get("entityType"));
            messageMap.put("entityId", data.get("entityId"));
            messageMap.put("postId", data.get("postId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT);
            messageMap.put("count", count);

            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
            messageMap.put("unread", unread);
        }
        model.addAttribute("commentNotice", messageMap);

        // 查询点赞类通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);
        messageMap = new HashMap<>();
        if (message != null) {
            messageMap.put("message", message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageMap.put("user", userService.findUserById((Integer) data.get("userId")));
            messageMap.put("entityType", data.get("entityType"));
            messageMap.put("entityId", data.get("entityId"));
            messageMap.put("postId", data.get("postId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_LIKE);
            messageMap.put("count", count);

            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
            messageMap.put("unread", unread);
        }
        model.addAttribute("likeNotice", messageMap);

        // 查询关注类通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);
        messageMap = new HashMap<>();
        if (message != null) {
            messageMap.put("message", message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageMap.put("user", userService.findUserById((Integer) data.get("userId")));
            messageMap.put("entityType", data.get("entityType"));
            messageMap.put("entityId", data.get("entityId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageMap.put("count", count);

            int unread = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageMap.put("unread", unread);
        }
        model.addAttribute("followNotice", messageMap);

        int letterTotalUnread = messageService.findUnreadLetterCount(user.getId(), null);
        model.addAttribute("letterTotalUnread", letterTotalUnread);
        // 查询未读通知数量
        int noticeTotalUnread = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeTotalUnread", noticeTotalUnread);

        return "/site/notice";
    }

    @RequestMapping(path = "/notice/detail/{topic}", method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic") String topic, Page page, Model model) {
        User user = hostHolder.getUser();

        page.setPath("/notice/detail" + topic);
        page.setLimit(5);
        page.setRows(messageService.findNoticeCount(user.getId(), topic));

        List<Message> notices = messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVO = new ArrayList<>();
        if (notices != null) {
            for (Message notice : notices) {
                Map<String, Object> map = new HashMap<>();
                // 通知
                map.put("notice", notice);
                // 内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                map.put("user", userService.findUserById((Integer) data.get("userId")));
                map.put("entityType", data.get("entityType"));
                map.put("entityId", data.get("entityId"));
                map.put("postId", data.get("postId"));
                // 通知作者
                map.put("fromUser", userService.findUserById(notice.getFromId()));

                noticeVO.add(map);
            }
        }

        model.addAttribute("notices", noticeVO);

        // 设置已读
        List<Integer> ids = getLetterIds(notices);
        if (!ids.isEmpty()){
            messageService.readMessage(ids);
        }

        return "/site/notice-detail";
    }
}
