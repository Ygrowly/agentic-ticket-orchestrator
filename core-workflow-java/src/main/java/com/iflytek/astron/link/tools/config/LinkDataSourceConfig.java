package com.iflytek.astron.link.tools.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = "com.iflytek.astron.link.tools.mapper", sqlSessionFactoryRef = "linkSqlSessionFactory")
public class LinkDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.link-datasource")
    public DataSourceProperties linkDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource linkDataSource(DataSourceProperties linkDataSourceProperties) {
        return linkDataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean
    public DataSourceTransactionManager linkTransactionManager(@Qualifier("linkDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public SqlSessionFactory linkSqlSessionFactory(@Qualifier("linkDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/link/*.xml"));
        return bean.getObject();
    }
}