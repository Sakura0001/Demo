package org.example.executor;

import org.redisson.api.RBlockingQueue;

public class DelayTaskQueueExecutor<T> {
    final private RBlockingQueue<T> queue;
    final private Thread msgLooper;
    private final DelayTaskQueueExecutor.Processor<T> processor;

    public interface Processor<T> {
        void process(T task) throws InterruptedException;


    }

    public DelayTaskQueueExecutor(String threadName, RBlockingQueue<T> queue, DelayTaskQueueExecutor.Processor<T> processor) {
        this.queue = queue;
        this.processor = processor;
        this.msgLooper = new Thread(this::looper);
        this.msgLooper.setName(threadName);
        this.msgLooper.start();
    }

    private void looper() {
        while(true) {
            try {
                T task = queue.take();
                processor.process(task);

            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {

            }
        }
    }

}