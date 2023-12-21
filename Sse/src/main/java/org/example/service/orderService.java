package org.example.service;

import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.example.util.SseUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
public class orderService {
    // 简单来个线程池
    ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    @Override
    public SseEmitter getOrderDetailById(String orderId, HttpServletResponse httpServletResponse) {
        // 建立连接
        SseEmitter emitter = SseUtil.getConnect(orderId);
        executor.execute(() -> {
            while (true) {
                log.error("=========SSE轮询中=========");
                try {
                    // 每5秒推送一次数据
                    Thread.sleep(5000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // 查询订单数据
                Torder torder = orderMapper.selectOne(Wrappers.lambdaQuery(Torder.class).eq(Torder::getOrderId, orderId));
                if (torder == null) {
                    // 如果订单不存在，返回错误，主动断开连接
                    SseUtil.sendMessage(orderId, JSON.toJSONString(ErrorCodeEnum.ORDER_ID_NOT_EXIST));
                    SseUtil.removeClientId(orderId);
                    break;
                }
                OrderDetailVO detailVO = new OrderDetailVO();
                detailVO.setIsExpire(stringRedisTemplate.opsForValue().get(orderId) == null);
                detailVO.setOrderId(orderId);
                detailVO.setCreateTime(torder.getCreateTime());
                detailVO.setOrderType(torder.getPolishType());
                detailVO.setAmount(torder.getAmount().doubleValue());
                // 根据不同的订单类型来封装不同的参数（这里为了满足产品的需求，想用一个接口显示不同种类订单的信息，用了SQL反模式设计数据库，导致代码很不优雅）
                if (torder.getOrderType() == 0) {
                    Wrapper<Object> statusByOrderId = getStatusByOrderId(orderId);
                    if (statusByOrderId.getCode() != 0) {
                        // 订单状态查询异常，返回错误，主动断开连接
                        SseUtil.sendMessage(orderId, JSON.toJSONString(ErrorCodeEnum.ASYNC_SERVICE_ERROR));
                        SseUtil.removeClientId(orderId);
                        break;
                    }
                    if (torder.getPolishType() == Common.POLISH_TYPE_WITH_PAPER) {
                        PaperStatusByOrderIdVO paperVO = (PaperStatusByOrderIdVO) statusByOrderId.getResult();
                        BeanUtils.copyProperties(paperVO, detailVO);
                        detailVO.setProgress(Double.valueOf(paperVO.getProgress()));
                        detailVO.setTitle(paperVO.getPaperTitle());
                        detailVO.setOrderStatus(paperVO.getStatus());
                    } else {
                        TextStatusByOrderIdVO textVO = (TextStatusByOrderIdVO) statusByOrderId.getResult();
                        BeanUtils.copyProperties(textVO, detailVO);
                        detailVO.setProgress(Double.valueOf(textVO.getProgress()));
                        detailVO.setTitle(textVO.getPaperTitle());
                        detailVO.setOrderStatus(textVO.getStatus());
                    }
                } else if (torder.getOrderType() == 1) {
                    CheckpassOrder checkpassOrder = checkpassOrderMapper.selectOne(Wrappers.lambdaQuery(CheckpassOrder.class).eq(CheckpassOrder::getOrderId, orderId));
                    CheckpassReport checkpassReport = checkpassReportMapper.selectOne(Wrappers.lambdaQuery(CheckpassReport.class).eq(CheckpassReport::getPaperId, checkpassOrder.getPaperId()));
                    detailVO.setOrderStatus(checkpassOrder.getStatus());
                    detailVO.setAuthor(checkpassReport.getAuthor());
                    detailVO.setTitle(checkpassReport.getTitle());
                    detailVO.setProgress(checkpassReport.getCopyPercent() == null ? 0 : checkpassReport.getCopyPercent());
                    detailVO.setCheckVersion(CommonUtil.getCheckVersion(checkpassOrder.getJaneName()));
                }
                boolean flag = SseUtil.sendMessage(orderId, JSON.toJSONString(detailVO));
                if (!flag) {
                    break;
                }
                if (torder.getStatus() == Common.ORDER_FINISH_STATUS) {
                    // 订单完成，主动关闭连接
                    try {
                        emitter.send(SseEmitter.event().reconnectTime(5000L).data("SSE关闭连接"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    SseUtil.removeClientId(orderId);
                    break;
                }
            }
        });
        return emitter;
}
