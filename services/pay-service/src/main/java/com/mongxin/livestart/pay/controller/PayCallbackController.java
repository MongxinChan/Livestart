package com.mongxin.livestart.pay.controller;

import com.mongxin.livestart.pay.service.PayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/pay/callback")
@RequiredArgsConstructor
public class PayCallbackController {
    private final PayService payService;

    @PostMapping("/alipay")
    public String alipay(@RequestParam Map<String, String> params) {
        try {
            payService.callback(params);
            return "success";
        } catch (Exception e) {
            return "fail";
        }
    }
}
