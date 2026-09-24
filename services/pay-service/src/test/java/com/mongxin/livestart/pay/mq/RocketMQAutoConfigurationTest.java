package com.mongxin.livestart.pay.mq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RocketMQAutoConfigurationTest {

    @Test
    void registersRocketMQTemplateWithSpringBoot3() {
        assertThat(ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).getCandidates())
                .contains(RocketMQAutoConfiguration.class.getName());

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RocketMQAutoConfiguration.class))
                .withPropertyValues("rocketmq.name-server=127.0.0.1:9876", "rocketmq.producer.group=pay-test")
                .withBean("defaultMQProducer", DefaultMQProducer.class, () -> mock(DefaultMQProducer.class))
                .run(context -> assertThat(context).hasSingleBean(RocketMQTemplate.class));
    }
}
