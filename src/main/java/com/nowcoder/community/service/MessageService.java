package com.nowcoder.community.service;

import com.nowcoder.community.dao.MessageMapper;
import com.nowcoder.community.entity.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;

    public List<Message> findLatestMessageByConversationId(int id, int offset, int limit) {
        // 对id进行拼接sql处理
//        String conversationId = "%" + id + "%";
        return messageMapper.selectLatestMessageByConversationId(id, offset, limit);
    }

    public int findConversationCount(int id) {
//        String conversationId = "%" + id + "%";
        return messageMapper.selectConversationRows(id);
    }
}
