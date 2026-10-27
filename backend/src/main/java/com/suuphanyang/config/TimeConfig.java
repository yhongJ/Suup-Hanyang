package com.suuphanyang.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {
    /** 현재 시각 기준. 테스트 시 고정 가능 */
    @Bean
    public Clock clock(AppProperties props) {
        return Clock.system(ZoneId.of(props.zone()));
    }
}
