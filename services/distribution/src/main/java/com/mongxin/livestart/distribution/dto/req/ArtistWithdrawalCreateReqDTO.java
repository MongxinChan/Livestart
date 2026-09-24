package com.mongxin.livestart.distribution.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 艺人提现申请请求 DTO。
 */
@Data
@Schema(description = "艺人提现申请请求")
public class ArtistWithdrawalCreateReqDTO {

    /** 客户端幂等请求号。 */
    @NotBlank(message = "提现请求号不能为空")
    @Schema(description = "提现请求幂等号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String requestNo;

    /** 提现金额。 */
    @NotNull(message = "提现金额不能为空")
    @DecimalMin(value = "1.00", message = "提现金额不能小于1元")
    @Schema(description = "提现金额", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    /** 收款账户类型。 */
    @NotBlank(message = "收款账户类型不能为空")
    @Schema(description = "收款账户类型，例如 ALIPAY", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountType;

    /** 收款账号。 */
    @NotBlank(message = "收款账号不能为空")
    @Schema(description = "收款账号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountNo;

    /** 收款人姓名。 */
    @Schema(description = "收款人姓名")
    private String accountName;
}
