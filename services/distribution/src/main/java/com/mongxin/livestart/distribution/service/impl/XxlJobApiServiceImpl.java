package com.mongxin.livestart.distribution.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mongxin.livestart.distribution.service.XxlJobApiService;
import com.mongxin.livestart.framework.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.HttpCookie;
import java.util.List;

/**
 * HTTP client for xxl-job-admin APIs.
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

    @Value("${xxl.job.executor.group-id:1}")
    private String jobGroupId;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Login to xxl-job-admin and extract the session cookie.
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
            if (cookies != null) {
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

        log.error("[XXL-JOB API] Failed to log in to xxl-job-admin. adminAddresses={}", adminAddresses);
        throw new ServiceException("Failed to log in to xxl-job-admin");
    }

    @Override
    public int addTicketReleaseJob(Long eventId, String eventTitle, String cronExpression) {
        int jobId = addOneShotJob("Ticket release - " + eventTitle, "ticketReleaseJobHandler",
                String.valueOf(eventId), cronExpression);
        log.info("[XXL-JOB API] Registered ticket release job. eventId={}, jobId={}, cron={}, executorAppname={}",
                eventId, jobId, cronExpression, executorAppname);
        return jobId;
    }

    @Override
    public int addTicketReminderJob(Long reminderId, String eventTitle, String cronExpression) {
        int jobId = addOneShotJob("Ticket reminder - " + eventTitle, "ticketReminderJobHandler",
                String.valueOf(reminderId), cronExpression);
        log.info("[XXL-JOB API] Registered ticket reminder job. reminderId={}, jobId={}, cron={}, executorAppname={}",
                reminderId, jobId, cronExpression, executorAppname);
        return jobId;
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

            log.info("[XXL-JOB API] Removed completed one-shot job. jobId={}", jobId);
        } catch (Exception e) {
            log.warn("[XXL-JOB API] Failed to remove job. jobId={}", jobId, e);
        }
    }

    private int addOneShotJob(String jobDesc, String executorHandler, String executorParam, String cronExpression) {
        String cookie = login();
        String url = adminAddresses + "/jobinfo/add";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("jobGroup", jobGroupId);
        params.add("jobDesc", jobDesc);
        params.add("scheduleType", "CRON");
        params.add("scheduleConf", cronExpression);
        params.add("glueType", "BEAN");
        params.add("executorHandler", executorHandler);
        params.add("executorParam", executorParam);
        params.add("executorRouteStrategy", "FIRST");
        params.add("misfireStrategy", "FIRE_ONCE_NOW");
        params.add("executorBlockStrategy", "SERIAL_EXECUTION");
        params.add("executorTimeout", "60");
        params.add("executorFailRetryCount", "2");
        params.add("triggerStatus", "1");
        params.add("author", "SYSTEM");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add(HttpHeaders.COOKIE, cookie);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        JSONObject result = JSON.parseObject(response.getBody());
        if (result != null && result.getIntValue("code") == 200) {
            return result.getIntValue("content");
        }

        log.error("[XXL-JOB API] Failed to register job. response={}", response.getBody());
        throw new ServiceException("Failed to register XXL-JOB task");
    }
}
