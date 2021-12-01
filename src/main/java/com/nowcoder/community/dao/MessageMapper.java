package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageMapper {

    // 查询当前用户的会话列表（每个会话只返回最新一条信息）
    List<Message> selectLatestMessageByConversationId(int userId, int offset, int limit);

    // 查询当前用户会话数量
    int selectConversationRows(int userId);

    // 查询某次会话所包含私信列表
    List<Message> selectLetters(String conversationId, int offset, int limit);

    // 查询某个会话所包含私信数量
    int selectLetterCount(String conversationId);

    // 查询未读私信的数量
    int selectUnreadLetterCount(int userId, String conversationId);

    // 添加私信
    int insertLetter(Message message);

    // 修改消息的状态
    int updateStatus(List<Integer> ids, int status);
}
