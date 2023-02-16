package com.imooc.service.websocket;

import com.alibaba.fastjson.JSONObject;
import com.imooc.bilibili.constat.UserMomentsConstant;
import com.imooc.bilibili.domain.Danmu;
import com.imooc.service.DanmuService;
import com.imooc.service.util.RockerMQUtil;
import com.imooc.service.util.TokenUtil;
import io.netty.util.internal.StringUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ServerEndpoint("/imserver{token}")
@Slf4j
@Data
public class WebSocketService {

//    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final AtomicInteger ONLINE_COUNT = new AtomicInteger(0);

    public static final ConcurrentHashMap<String, WebSocketService> WEBSOCKET_MAP = new ConcurrentHashMap<>();

    private Session session;

    private String sessionId;

    private Long userId;

    private static ApplicationContext APPLICATION_CONTEXT;


    public static void setApplicationContext(ApplicationContext applicationContext){
        WebSocketService.APPLICATION_CONTEXT = applicationContext;
    }

    @OnOpen
    public void openConnection(Session session, @PathParam("token") String token){
        try{
            this.userId = TokenUtil.verifyToken(token);
        }catch (Exception e){ }
        this.sessionId = session.getId();
        this.session = session;
        if(WEBSOCKET_MAP.containsKey(this.sessionId)){
            WEBSOCKET_MAP.remove(this.sessionId);
            WEBSOCKET_MAP.put(this.sessionId, this);
        }else {
            WEBSOCKET_MAP.put(this.sessionId, this);
            ONLINE_COUNT.getAndIncrement();
        }
        log.info("用户连接成功： " + this.sessionId + ",当前在线人数为： " + ONLINE_COUNT.get());
        try {
            this.sendMessage("0");
        }catch (Exception e){
            log.error("连接异常");
        }
    }

    @OnClose
    public void closeConnection(){
        if (WEBSOCKET_MAP.containsKey(this.sessionId)){
            WEBSOCKET_MAP.remove(this.sessionId);
            ONLINE_COUNT.getAndDecrement();
        }
        log.info("用户退出： " + this.sessionId + ",当前在线人数为： " + ONLINE_COUNT.get());
    }

    @OnMessage
    public void OnMessage(String message){
        log.info("用户信息： " + this.sessionId + ",报文" + message);
        if (!StringUtil.isNullOrEmpty(message)){
            try {
                // 群发消息
                for(Map.Entry<String, WebSocketService> entry : WEBSOCKET_MAP.entrySet()){
                    WebSocketService webSocketService = entry.getValue();
                    // 生产者
                    DefaultMQProducer danmusProducer = (DefaultMQProducer) APPLICATION_CONTEXT.getBean("danmusProducer");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("message", message);
                    jsonObject.put("sessionId", webSocketService.getSessionId());
                    Message msg = new Message(UserMomentsConstant.TOPIC_DANMUS,
                            jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8));
                    RockerMQUtil.asyncSendMag(danmusProducer, msg);
                }
                if (this.userId != null){
                    // 保存弹幕到数据库
                    Danmu danmu = JSONObject.parseObject(message, Danmu.class);
                    danmu.setUserId(this.userId);
                    danmu.setCreateTime(new Date());
                    DanmuService danmuService = (DanmuService) APPLICATION_CONTEXT.getBean("danmuService");
                    danmuService.asyncAddDanmu(danmu);
                    // 保存弹幕到Redis
                    danmuService.addDanmusToRedis(danmu);
                }
            }catch (Exception e){
                log.error("弹幕接收出现问题");
                e.printStackTrace();
            }
        }
    }

    @OnError
    public void OnError(Throwable error){

    }

    /**
     * 间隔时间5s
     * @throws IOException
     */
    @Scheduled(fixedRate = 5000)
    private void noticeOnlineCount0() throws IOException{
        for(Map.Entry<String, WebSocketService> entry : WEBSOCKET_MAP.entrySet()) {
            WebSocketService webSocketService = entry.getValue();
            if (webSocketService.session.isOpen()){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("onlineCount", ONLINE_COUNT.get());
                jsonObject.put("msg", "当前在线人数为" +ONLINE_COUNT.get());
                webSocketService.sendMessage(jsonObject.toJSONString());
            }
        }
    }

    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }



}
