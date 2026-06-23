package com.mongxin.livestart.engine.common.biz.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 鐢ㄦ埛淇℃伅浼犻€?DTO锛堜粠缃戝叧娉ㄥ叆鐨?Header 涓В鏋愶級
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {

    /**
     * 鐢ㄦ埛ID
     */
    private String userId;

    /**
     * 鐢ㄦ埛鍚?
     */
    private String username;

    /**
     * 鎵嬫満鍙?
     */
    private String phone;

    /**
     * 鐢ㄦ埛绫诲瀷 1:涔愯糠 2:鑹轰汉 3:鍦哄湴绠＄悊鍛?4:瓒呯
     */
    private Integer userType;
}
