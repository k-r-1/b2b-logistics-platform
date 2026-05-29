package com.boxoffice.ainotificationservice.ai.deadline;

import com.boxoffice.ainotificationservice.ai.exception.AiErrorCode;
import com.boxoffice.common.exception.BaseException;

public record OrderLine(String productName, int quantity) {

    public OrderLine {
        if (productName == null || productName.isBlank() || quantity <= 0) {
            throw new BaseException(AiErrorCode.INVALID_DISPATCH_INPUT);
        }
    }
}
