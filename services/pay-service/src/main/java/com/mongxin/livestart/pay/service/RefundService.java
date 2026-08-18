package com.mongxin.livestart.pay.service;

import com.mongxin.livestart.pay.dto.RefundCreateRequest;
import com.mongxin.livestart.pay.dto.RefundCreateResponse;

public interface RefundService {
    RefundCreateResponse create(RefundCreateRequest request, Long userId);
}
