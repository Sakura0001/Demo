package org.example.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.cluster.nodes}")
    private List<String> clusterNodes; // Redis 集群节点列表

    @Value("${spring.redis.password}")
    private String REDIS_PASSWORD;

    @Bean
    public RedissonClient createRedisApi() {
        Config redissonConfig = new Config();
        redissonConfig.setCodec(new org.redisson.client.codec.StringCodec());

        // 设置集群模式
        ClusterServersConfig clusterServersConfig = redissonConfig.useClusterServers();
        // 添加集群节点
        for (String nodeAddress : clusterNodes) {
            clusterServersConfig.addNodeAddress(String.format("redis://%s", nodeAddress));
        }

//        clusterServersConfig.setPassword(REDIS_PASSWORD);
        // 其他集群配置...
        // 例如：设置连接超时时间、读取操作的超时时间、尝试重连次数等

        return Redisson.create(redissonConfig);
    }
}
