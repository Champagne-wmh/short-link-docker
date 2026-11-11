package com.shortlink.mq;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
public class Producer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void initCallback() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            String id = correlationData != null ? correlationData.getId() : "unknown";
            if (ack) {
                log.debug("消息到达 Broker, id={}", id);
            } else {
                // confirm=false 说明消息没到 Broker（交换机不存在/磁盘满/网络断）
                // 生产环境：落一张 t_mq_send_fail 表，定时任务重发
                log.error("消息发送失败, id={}, cause={}", id, cause);
            }
        });
        rabbitTemplate.setReturnsCallback(returned -> {
            // confirm=true 但 return 回来，说明交换机存在但路由不到队列（routingKey 写错/队列不存在）
            log.error("路由失败, replyText={}, routingKey={}, body={}",
                    returned.getReplyText(),
                    returned.getRoutingKey(),
                    new String(returned.getMessage().getBody(), StandardCharsets.UTF_8));
        });
    }

    public void sendOrderObj(Object obj) {
        CorrelationData cd = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDER_EXCHANGE,
                RabbitConfig.ORDER_ROUTING_KEY,
                obj,
                cd
        );
    }
}
