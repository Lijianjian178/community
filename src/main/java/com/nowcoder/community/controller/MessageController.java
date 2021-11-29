package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        List<Message> messages = messageService.findLatestMessageByConversationId(user.getId(), page.getOffset(), page.getLimit());
        List<Map<String, Object>> messageVoList = new ArrayList<>();
        if (messages != null) {
            for (Message message : messages) {
                // 信息
                Map<String, Object> map = new HashMap<>();
                map.put("message", message);

                // 对方用户
                int counterPartId = Integer.parseInt(message.getConversationId().substring(message.getConversationId().indexOf("_") + 1));
                User counterPartUser = userService.findUserById(counterPartId);
                map.put("cpUser", counterPartUser);

                messageVoList.add(map);
            }
        }

        model.addAttribute("messages", messageVoList);
        return "/site/letter";
    }
}
