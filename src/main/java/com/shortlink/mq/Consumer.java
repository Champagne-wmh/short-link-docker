package com.shortlink.mq;



import com.shortlink.dto.ShortLinkAccessMsg;
import com.shortlink.service.impl.LinkAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.Channel;

import java.io.IOException;


@Slf4j
@Component
@RequiredArgsConstructor
public class Consumer {

    // 注入刚才写的 Service
    private final LinkAccessService linkAccessService;

    @RabbitListener(queues = RabbitConfig.ORDER_QUEUE)
    public void handleAccessMsg(ShortLinkAccessMsg msg, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("【消费者】收到点击日志: msgId={}, shortCode={}", msg.getMsgId(), msg.getShortCode());

        try {
            // 🚀 核心：调用 Service
            linkAccessService.handle(msg);

            // 成功，手动 ACK
            channel.basicAck(deliveryTag, false);

        } catch (DuplicateKeyException e) {
            // 如果是唯一索引冲突（重复消费），不管它，直接 ACK
            log.warn("【消费者】重复消息，直接丢弃。msgId={}", msg.getMsgId());
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("【消费者】处理异常", e);
            // 数据库真报错了，Nack 进死信
            channel.basicNack(deliveryTag, false, false);
        }
    }
}





//    @RabbitListener(queues =  RabbitConfig.DLX_QUEUE)
//    public void receiveDlxMsg(Message message,Channel channel,
//                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)throws IOException{
//        try{
//            channel.basicAck(deliveryTag, false);
//        }catch (Exception e){
//            log.error("死信处理失败", e);
//            channel.basicNack(deliveryTag, false, false);
//        }
//    }
//}
//package com.shortlink.mq;
//
//import com.rabbitmq.client.Channel;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.amqp.core.Message;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.amqp.support.AmqpHeaders;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;

//@Slf4j
//@Component
//public class Consumer {
//
//    // 注意：参数里加入了 Channel 和 deliveryTag
//    @RabbitListener(queues = RabbitConfig.ORDER_QUEUE)
//    public void receiveMsg(String msg, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
//        log.info("【消费者】接收到了消息: {}", msg);
//
//        try {
//            // ================= 你的业务逻辑 =================
//            // 假设这里是你处理短链点击的逻辑
//            if (msg.contains("搞破坏")) {
//                throw new RuntimeException("业务处理失败了！");
//            }
//            // ==============================================
//
//            // 业务执行成功：手动 ACK 告诉 MQ 消息已成功消费
//            // 参数1: deliveryTag (消息编号)
//            // 参数2: multiple (是否批量确认，false 表示只确认当前这一条)
//            channel.basicAck(deliveryTag, false);
//            log.info("业务处理成功，手动 ACK 完成");
//
//        } catch (Exception e) {
//            log.error("业务处理异常，开始 Nack...", e);
//
//            // 业务执行失败：手动 NACK
//            // 参数1: deliveryTag
//            // 参数2: multiple (false)
//            // 参数3: requeue (是否重新入队，极度重要，看下方解释)
//            channel.basicNack(deliveryTag, false, false);
//            log.info("已拒绝消息，且不重新入队");
//        }
//    }
//}
