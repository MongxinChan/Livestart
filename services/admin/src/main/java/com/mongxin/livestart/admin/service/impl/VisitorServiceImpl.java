package com.mongxin.livestart.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.admin.common.biz.user.UserContext;
import com.mongxin.livestart.admin.common.convention.exception.ClientException;
import com.mongxin.livestart.admin.common.enums.VisitorErrorCodeEnum;
import com.mongxin.livestart.admin.dao.entity.UserVisitorDO;
import com.mongxin.livestart.admin.dao.mapper.UserVisitorMapper;
import com.mongxin.livestart.admin.dto.req.VisitorAddReqDTO;
import com.mongxin.livestart.admin.dto.req.VisitorUpdateReqDTO;
import com.mongxin.livestart.admin.dto.resp.VisitorRespDTO;
import com.mongxin.livestart.admin.service.VisitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 常用观演人接口实现层
 *
 * @author Mongxin
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitorServiceImpl extends ServiceImpl<UserVisitorMapper, UserVisitorDO>
        implements VisitorService {

    /**
     * AES 加密密钥，从配置文件注入（必须 16/24/32 位）
     * 生产环境应通过配置中心或密钥管理服务注入，绝不可硬编码
     */
    @Value("${livestart.visitor.aes-key:LiveStart2025Key!}")
    private String aesKey;

    // ========================= 证件类型常量 =========================

    private static final int CARD_TYPE_ID_CARD = 1;
    private static final int CARD_TYPE_PASSPORT = 2;
    private static final int CARD_TYPE_HK_MACAO = 3;
    private static final int CARD_TYPE_TAIWAN = 4;

    private static final Map<Integer, String> CARD_TYPE_DESC_MAP = Map.of(
            CARD_TYPE_ID_CARD, "身份证",
            CARD_TYPE_PASSPORT, "护照",
            CARD_TYPE_HK_MACAO, "港澳通行证",
            CARD_TYPE_TAIWAN, "台胞证"
    );

    // ========================= 身份证校验常量 =========================

    /** 合法省份代码（前两位） */
    private static final Set<String> VALID_PROVINCE_CODES = Set.of(
            "11", "12", "13", "14", "15",
            "21", "22", "23",
            "31", "32", "33", "34", "35", "36", "37",
            "41", "42", "43", "44", "45", "46",
            "50", "51", "52", "53", "54",
            "61", "62", "63", "64", "65",
            "71", "81", "82"
    );

    /** ISO 7064:1983 MOD 11-2 加权因子 */
    private static final int[] ID_CARD_WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

    /** ISO 7064:1983 MOD 11-2 校验码映射 */
    private static final char[] ID_CARD_CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    // ================================================================

    @Override
    public void addVisitor(VisitorAddReqDTO requestParam) {
        Long userId = getCurrentUserId();

        // 1. 证件格式校验
        validateCardNo(requestParam.getCardType(), requestParam.getCardNo());

        // 2. 生成 hash 用于判重（对明文做 SHA-256）
        String cardNoHash = sha256(requestParam.getCardNo());

        // 3. 判重：同一用户下同一证件不可重复
        boolean exists = baseMapper.selectCount(
                Wrappers.lambdaQuery(UserVisitorDO.class)
                        .eq(UserVisitorDO::getUserId, userId)
                        .eq(UserVisitorDO::getCardNoHash, cardNoHash)
                        .eq(UserVisitorDO::getDelFlag, 0)
        ) > 0;
        if (exists) {
            throw new ClientException(VisitorErrorCodeEnum.VISITOR_CARD_DUPLICATE);
        }

        // 4. AES 加密证件号入库
        String encryptedCardNo = aesEncrypt(requestParam.getCardNo());

        // 5. 组装并持久化
        UserVisitorDO visitorDO = BeanUtil.toBean(requestParam, UserVisitorDO.class);
        visitorDO.setUserId(userId);
        visitorDO.setCardNo(encryptedCardNo);
        visitorDO.setCardNoHash(cardNoHash);

        int inserted = baseMapper.insert(visitorDO);
        if (inserted < 1) {
            throw new ClientException(VisitorErrorCodeEnum.VISITOR_SAVE_ERROR);
        }
    }

    @Override
    public void updateVisitor(VisitorUpdateReqDTO requestParam) {
        Long userId = getCurrentUserId();
        // 鉴权：确认该观演人属于当前用户
        getAndCheckOwnership(requestParam.getId(), userId);

        // 证件号不允许修改，仅支持更新 realName / mobile
        LambdaUpdateWrapper<UserVisitorDO> updateWrapper = Wrappers.lambdaUpdate(UserVisitorDO.class)
                .eq(UserVisitorDO::getId, requestParam.getId())
                .eq(UserVisitorDO::getUserId, userId);

        if (StrUtil.isNotBlank(requestParam.getRealName())) {
            updateWrapper.set(UserVisitorDO::getRealName, requestParam.getRealName());
        }
        if (StrUtil.isNotBlank(requestParam.getMobile())) {
            updateWrapper.set(UserVisitorDO::getMobile, requestParam.getMobile());
        }
        baseMapper.update(null, updateWrapper);
    }

    @Override
    public void removeVisitor(Long id) {
        Long userId = getCurrentUserId();
        // 鉴权：确认该观演人属于当前用户
        getAndCheckOwnership(id, userId);

        // 逻辑删除
        LambdaUpdateWrapper<UserVisitorDO> updateWrapper = Wrappers.lambdaUpdate(UserVisitorDO.class)
                .eq(UserVisitorDO::getId, id)
                .eq(UserVisitorDO::getUserId, userId)
                .set(UserVisitorDO::getDelFlag, 1);
        baseMapper.update(null, updateWrapper);
    }

    @Override
    public List<VisitorRespDTO> listVisitors() {
        Long userId = getCurrentUserId();
        LambdaQueryWrapper<UserVisitorDO> queryWrapper = Wrappers.lambdaQuery(UserVisitorDO.class)
                .eq(UserVisitorDO::getUserId, userId)
                .eq(UserVisitorDO::getDelFlag, 0)
                .orderByDesc(UserVisitorDO::getCreateTime);
        List<UserVisitorDO> doList = baseMapper.selectList(queryWrapper);
        return doList.stream().map(this::toRespDTO).collect(Collectors.toList());
    }

    @Override
    public VisitorRespDTO getVisitorById(Long id) {
        Long userId = getCurrentUserId();
        UserVisitorDO visitorDO = getAndCheckOwnership(id, userId);
        return toRespDTO(visitorDO);
    }

    // ========================= 私有方法 =========================

    /**
     * 获取当前登录用户 ID，若未登录则抛异常
     */
    private Long getCurrentUserId() {
        String userIdStr = UserContext.getUserId();
        if (StrUtil.isBlank(userIdStr)) {
            throw new ClientException("用户未登录");
        }
        return Long.parseLong(userIdStr);
    }

    /**
     * 查询观演人并校验归属权
     */
    private UserVisitorDO getAndCheckOwnership(Long id, Long userId) {
        LambdaQueryWrapper<UserVisitorDO> queryWrapper = Wrappers.lambdaQuery(UserVisitorDO.class)
                .eq(UserVisitorDO::getId, id)
                .eq(UserVisitorDO::getDelFlag, 0);
        UserVisitorDO visitorDO = baseMapper.selectOne(queryWrapper);
        if (visitorDO == null) {
            throw new ClientException(VisitorErrorCodeEnum.VISITOR_NOT_FOUND);
        }
        if (!userId.equals(visitorDO.getUserId())) {
            throw new ClientException(VisitorErrorCodeEnum.VISITOR_NOT_BELONG_TO_USER);
        }
        return visitorDO;
    }

    /**
     * 将 DO 转换为 RespDTO，并填充证件类型描述和解密证件号
     */
    private VisitorRespDTO toRespDTO(UserVisitorDO visitorDO) {
        VisitorRespDTO respDTO = BeanUtil.toBean(visitorDO, VisitorRespDTO.class);
        respDTO.setCardTypeDesc(CARD_TYPE_DESC_MAP.getOrDefault(visitorDO.getCardType(), "未知"));
        // 解密后由 @JsonSerialize 脱敏序列化器自动处理脱敏输出
        respDTO.setCardNo(aesDecrypt(visitorDO.getCardNo()));
        return respDTO;
    }

    // ========================= 证件校验 =========================

    /**
     * 根据证件类型分发校验
     */
    private void validateCardNo(Integer cardType, String cardNo) {
        if (cardType == null || StrUtil.isBlank(cardNo)) {
            throw new ClientException(VisitorErrorCodeEnum.VISITOR_CARD_FORMAT_ERROR);
        }
        boolean valid = switch (cardType) {
            case CARD_TYPE_ID_CARD -> validateIdCard(cardNo);
            case CARD_TYPE_PASSPORT -> validatePassport(cardNo);
            case CARD_TYPE_HK_MACAO -> validateHkMacao(cardNo);
            case CARD_TYPE_TAIWAN -> validateTaiwan(cardNo);
            default -> throw new ClientException("不支持的证件类型");
        };
        if (!valid) {
            throw new ClientException(VisitorErrorCodeEnum.VISITOR_CARD_FORMAT_ERROR);
        }
    }

    /**
     * 身份证校验（18位）
     * 规则：格式 + 省份代码 + 出生日期 + ISO 7064:1983 MOD 11-2 校验码
     */
    private boolean validateIdCard(String cardNo) {
        if (cardNo.length() != 18) return false;
        String upperCardNo = cardNo.toUpperCase();

        // 前17位必须是数字
        for (int i = 0; i < 17; i++) {
            if (!Character.isDigit(upperCardNo.charAt(i))) return false;
        }
        // 末位必须是数字或 X
        char lastChar = upperCardNo.charAt(17);
        if (!Character.isDigit(lastChar) && lastChar != 'X') return false;

        // 省份代码校验
        String provinceCode = upperCardNo.substring(0, 2);
        if (!VALID_PROVINCE_CODES.contains(provinceCode)) return false;

        // 出生日期校验（第7~14位：yyyyMMdd）
        String birthDateStr = upperCardNo.substring(6, 14);
        try {
            LocalDate birthDate = LocalDate.parse(birthDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            if (birthDate.isAfter(LocalDate.now())) return false;
        } catch (DateTimeParseException e) {
            return false;
        }

        // ISO 7064:1983 MOD 11-2 校验码验证
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (upperCardNo.charAt(i) - '0') * ID_CARD_WEIGHTS[i];
        }
        char expectedCheckCode = ID_CARD_CHECK_CODES[sum % 11];
        return upperCardNo.charAt(17) == expectedCheckCode;
    }

    /**
     * 中国护照校验：E 开头 + 8位数字（共9位）
     */
    private boolean validatePassport(String cardNo) {
        return cardNo.matches("^[Ee]\\d{8}$");
    }

    /**
     * 港澳通行证校验：H 或 M 开头 + 8或10位数字
     */
    private boolean validateHkMacao(String cardNo) {
        return cardNo.matches("^[HhMm]\\d{8}(\\d{2})?$");
    }

    /**
     * 台胞证校验：8位数字
     */
    private boolean validateTaiwan(String cardNo) {
        return cardNo.matches("^\\d{8}$");
    }

    // ========================= 加密工具 =========================

    /**
     * AES-128 加密（CBC 模式，Hutool SecureUtil）
     */
    private String aesEncrypt(String plainText) {
        try {
            AES aes = SecureUtil.aes(aesKey.getBytes(StandardCharsets.UTF_8));
            return aes.encryptBase64(plainText);
        } catch (Exception e) {
            log.error("AES 加密失败", e);
            throw new ClientException("证件号加密处理失败");
        }
    }

    /**
     * AES-128 解密
     */
    private String aesDecrypt(String cipherText) {
        try {
            AES aes = SecureUtil.aes(aesKey.getBytes(StandardCharsets.UTF_8));
            return aes.decryptStr(cipherText);
        } catch (Exception e) {
            log.error("AES 解密失败", e);
            return "****";
        }
    }

    /**
     * SHA-256 哈希（用于判重索引，不可逆）
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.toUpperCase().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }
}
