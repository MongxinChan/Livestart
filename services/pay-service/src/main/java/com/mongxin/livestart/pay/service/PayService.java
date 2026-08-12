package com.mongxin.livestart.pay.service;

import com.mongxin.livestart.pay.dto.PayCreateRequest;
import com.mongxin.livestart.pay.dto.PayCreateResponse;

public interface PayService {
    PayCreateResponse create(PayCreateRequest request, Long userId);

    void callback(java.util.Map<String, String> params);
}
