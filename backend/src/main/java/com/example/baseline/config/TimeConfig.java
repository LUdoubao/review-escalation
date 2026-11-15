package com.example.baseline.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 业务时钟配置：为「今天是哪天」「现在几点」提供唯一来源。
 *
 * <p>领域：任务看板的时间口径；关系：被 {@code TaskQueryService} 与 {@code TaskBoardService}
 * 注入，用于截止日校验、逾期判定与时间戳写入。
 * 场景：容器的 JVM 默认时区可能是 UTC（CI、容器镜像常见），而业务日期按 Asia/Shanghai 计。
 * 若直接调用 {@code LocalDate.now()}，部署时区一旦不同，截止日校验与逾期统计会在本地
 * 00:00-08:00 区间错判一天。这里把时区固定为业务时区，并与数据源
 * {@code serverTimezone=Asia/Shanghai} 保持一致。
 * 生命周期：单例 Bean；{@link Clock} 不可变、线程安全。</p>
 */
@Configuration
public class TimeConfig {

    /** 业务时区：与数据源连接串的 serverTimezone 保持一致，改动需同时调整该配置与文档。 */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 业务时钟，供各 service 取当前日期与时刻。
     *
     * <p>以 Bean 形式暴露而非静态工具方法，便于测试替换为固定时钟。</p>
     *
     * @return 固定业务时区的系统时钟
     */
    @Bean
    public Clock businessClock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
