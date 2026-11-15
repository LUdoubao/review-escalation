package com.example.baseline.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件装配。
 *
 * <p>领域：任务看板持久化基础设施；关系：由 Spring 在启动期装配为单例拦截器链，
 * 对 {@code BoardTaskMapper} 的写操作生效，与具体业务解耦。
 * 场景：任务编辑与移动依赖乐观锁插件为 {@code @Version} 字段生成
 * {@code WHERE version = ?} 条件，并在成功后自增版本，防止并发覆盖。
 * 生命周期：容器启动时创建一次，运行期只读。</p>
 *
 * <p>分页未启用分页插件：看板与列表的分页条件由 mapper XML 显式书写
 * （LIMIT / OFFSET 与独立计数查询），可避免引入 jsqlparser 依赖，
 * 也让 SQL 中的条件集合可以直接逐条核对。</p>
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器链。
     *
     * @return 含乐观锁拦截器的插件实例，由容器管理为单例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 乐观锁拦截器：仅对带 @Version 字段的实体在 updateById 时生效。
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
