package org.example.service;

import org.example.executor.DelayTaskQueueExecutor;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.sql.SQLOutput;
import java.util.concurrent.TimeUnit;

@Service
public class DelayQueueService {

    @Autowired
    private RedissonClient redisson;

    private RDelayedQueue<String> delayedQueue;

    @PostConstruct
    public void initDelayQueue() {
        RBlockingQueue<String> blockingQueue = redisson.getBlockingQueue("orderDelayQueue");
        delayedQueue = redisson.getDelayedQueue(blockingQueue);
        new DelayTaskQueueExecutor<>("ORDER_DELAY", blockingQueue,this::processOrder);
    }
    public void processOrder(String orderId) {
        System.out.println(orderId+""+"已完成");
    }
    /**
     * 将订单信息加入到延迟队列中，并设置TTL
     *
     * @param orderId 订单信息
     */
    public void addToDelayQueue(String orderId) {
        delayedQueue.offer(orderId, 1, TimeUnit.MINUTES);
    }




    public void processOrder2(String orderId) {
        System.out.println(orderId+""+"已完成");
    }
}