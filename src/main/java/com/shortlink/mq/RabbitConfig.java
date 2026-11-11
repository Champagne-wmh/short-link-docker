package com.shortlink.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    //==================一般业务队列配置
    public static final String ORDER_QUEUE = "order_queue";

    public static final String ORDER_EXCHANGE = "order_exchange";

    public static final String ORDER_ROUTING_KEY = "order_routing_key";

    //=================死信队列配置
    public static final String DLX_EXCHANGE = "dlx_exchange";

    public static final String DLX_QUEUE = "dlx_queue";

    public static final String DLX_ROUTING_KEY = "dlx_routing_key";

    //===============正常队列
    @Bean
    public Queue orderQueue(){
        return QueueBuilder.durable(ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange orderExchange(){
        return ExchangeBuilder.directExchange(ORDER_EXCHANGE).durable(true).build();
    }

    @Bean
    public Binding orderBinding(Queue orderQueue,DirectExchange orderExchange){
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ORDER_ROUTING_KEY);
    }

    //================死信队列
    @Bean
    public Queue dlxQueue(){
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    @Bean
    public DirectExchange dlxExchange(){
        return ExchangeBuilder.directExchange(DLX_EXCHANGE).durable(true).build();
    }
    
    @Bean
    public Binding dlxBinding(Queue dlxQueue,DirectExchange dlxExchange){
        return BindingBuilder.bind(dlxQueue).to(dlxExchange).with(DLX_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}



