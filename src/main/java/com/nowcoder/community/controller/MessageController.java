package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @RequestMapping(path = "/list", method = RequestMethod.GET)
    public String getMessagePage(Model model, Page page) {
        // 获取当前用户
        User user = hostHolder.getUser();

        // 分页信息
        page.setLimit(5);
        page.setPath("/message/list");
        page.setRows(messageService.findConversationCount(user.getId()));
        System.out.println(messageService.findConversationCount(user.getId()));

        int totalUnread = 0;
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
                totalUnread += unread;

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
        model.addAttribute("totalUnread", totalUnread);
        return "/site/letter";
    }

    @RequestMapping(path = "/letter-detail/{conversationId}", method = RequestMethod.GET)
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

        return "/site/letter-detail";
    }

    @RequestMapping(path = "/send", method = RequestMethod.POST)
    @ResponseBody
    public String addMessage(String toUsername, String content) {
        User user = hostHolder.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(403, "还没有登录！");
        }

        User toUser = userService.findUserByName(toUsername);
        System.out.println(toUser);
        if (toUser == null){
            return CommunityUtil.getJSONString(403, "发送对象不存在！");
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

    @RequestMapping(path = "/delete_msg/{conversationId}/{id}", method = RequestMethod.GET)
    public String deleteMessage(@PathVariable("id") int id, @PathVariable("conversationId") String conversationId) {
        messageService.deleteMessage(id);
        return "redirect:/message/letter-detail/" + conversationId;
    }
}
