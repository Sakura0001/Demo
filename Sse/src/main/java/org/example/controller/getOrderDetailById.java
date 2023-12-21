package org.example.controller;

import org.example.service.orderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;

@RestController
public class getOrderDetailById {

    @Autowired
    orderService orderService ;

    @CrossOrigin
    @GetMapping("/getOrderDetail")
    public SseEmitter getOrderDetailById(String orderId, HttpServletResponse httpServletResponse) {
        httpServletResponse.setContentType("text/event-stream");
        httpServletResponse.setCharacterEncoding("utf-8");
        return orderService.getOrderDetailById(orderId, httpServletResponse);
    }
}
