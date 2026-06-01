package com.mongxin.livestart.distribution.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mongxin.livestart.distribution.service.XxlJobApiService;
import com.mongxin.livestart.framework.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.HttpCookie;
import java.util.List;

/**
 * XXL-JOB Admin OpenAPI 客户端实现
 * <p>
 * 通过 HTTP 调用 xxl-job-admin 的 REST 接口，动态注册/移除一次性定时任务。
 * XXL-JOB 2.x 的 API 需要先登录获取 Cookie，再携带 Cookie 调用业务接口。
 */
@Slf4j
@Service
public class XxlJobApiServiceImpl implements XxlJobApiService {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.admin.username:admin}")
    private String adminUsername;

    @Value("${xxl.job.admin.password:123456}")
    private String adminPassword;

    @Value("${xxl.job.executor.appname}")
    private String executorAppname;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 登录 xxl-job-admin 获取会话 Cookie
     */
    private String login() {
        String loginUrl = adminAddresses + "/login";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("userName", adminUsername);
        params.add("password", adminPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(loginUrl, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (cookies != null && !cookies.isEmpty()) {
                // 解析 XXL_JOB_LOGIN_IDENTITY cookie
                for (String cookieStr : cookies) {
                    List<HttpCookie> parsed = HttpCookie.parse(cookieStr);
                    for (HttpCookie cookie : parsed) {
                        if ("XXL_JOB_LOGIN_IDENTITY".equals(cookie.getName())) {
                            return cookie.getName() + "=" + cookie.getValue();
                        }
                    }
                }
            }
        }

        log.error("[XXL-JOB API] 登录 xxl-job-admin 失败，地址={}", adminAddresses);
        throw new ServiceException("XXL-JOB 调度中心登录失败");
    }

    @Override
    public int addTicketReleaseJob(Long eventId, String eventTitle, String cronExpression) {
        String cookie = login();
        String url = adminAddresses + "/jobinfo/add";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("jobGroup", "1");                                          // 执行器分组ID（默认1，首个注册的执行器）
        params.add("jobDesc", "门票开售-" + eventTitle);                       // 任务描述
        params.add("scheduleType", "CRON");                                   // 调度类型
        params.add("scheduleConf", cronExpression);                           // Cron 表达式
        params.add("glueType", "BEAN");                                       // 运行模式
        params.add("executorHandler", "ticketReleaseJobHandler");             // JobHandler 名称
        params.add("executorParam", String.valueOf(eventId));                 // 执行参数（演出ID）
        params.add("executorRouteStrategy", "FIRST");                         // 路由策略
        params.add("misfireStrategy", "FIRE_ONCE_NOW");                       // 错过触发：立即补偿执行一次
        params.add("executorBlockStrategy", "SERIAL_EXECUTION");              // 阻塞策略
        params.add("executorTimeout", "60");                                  // 超时时间（秒）
        params.add("executorFailRetryCount", "2");                            // 失败重试次数
        params.add("triggerStatus", "1");                                     // 创建后立即启用
        params.add("author", "SYSTEM");                                       // 负责人

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add(HttpHeaders.COOKIE, cookie);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        JSONObject result = JSON.parseObject(response.getBody());
        if (result != null && result.getIntValue("code") == 200) {
            int jobId = result.getIntValue("content");
            log.info("[XXL-JOB API] 动态注册门票开售任务成功，eventId={}，jobId={}，cron={}", eventId, jobId, cronExpression);
            return jobId;
        }

        log.error("[XXL-JOB API] 注册任务失败，响应={}", response.getBody());
        throw new ServiceException("XXL-JOB 定时任务注册失败");
    }

    @Override
    public void removeJob(int jobId) {
        try {
            String cookie = login();
            String url = adminAddresses + "/jobinfo/remove?id=" + jobId;

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.COOKIE, cookie);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.postForEntity(url, request, String.class);

            log.info("[XXL-JOB API] 移除已完成的一次性任务成功，jobId={}", jobId);
        } catch (Exception e) {
            // 移除失败不影响主流程，仅记录日志
            log.warn("[XXL-JOB API] 移除任务异常（不影响业务），jobId={}", jobId, e);
        }
    }
}
