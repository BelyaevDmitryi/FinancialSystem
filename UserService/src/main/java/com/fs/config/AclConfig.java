package com.fs.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.acls.AclPermissionCacheOptimizer;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl;
import org.springframework.security.acls.domain.ConsoleAuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionFactory;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class AclConfig {

    private final DataSource dataSource;

    public AclConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("aclCache");
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(3600, TimeUnit.SECONDS)
                .expireAfterAccess(1800, TimeUnit.SECONDS);
    }

    @Bean
    public AclCache aclCacheBean(CacheManager cacheManager,
                                  PermissionGrantingStrategy permissionGrantingStrategy,
                                  AclAuthorizationStrategy aclAuthorizationStrategy) {
        return new SpringCacheBasedAclCache(
                cacheManager.getCache("aclCache"),
                permissionGrantingStrategy,
                aclAuthorizationStrategy
        );
    }

    @Bean
    public PermissionGrantingStrategy permissionGrantingStrategy() {
        return new DefaultPermissionGrantingStrategy(new ConsoleAuditLogger());
    }

    @Bean
    public AclAuthorizationStrategy aclAuthorizationStrategy() {
        return new AclAuthorizationStrategyImpl(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
    }

    @Bean
    public LookupStrategy lookupStrategy(AclCache aclCacheBean,
                                         AclAuthorizationStrategy aclAuthorizationStrategy) {
        BasicLookupStrategy lookupStrategy = new BasicLookupStrategy(
                dataSource,
                aclCacheBean,
                aclAuthorizationStrategy,
                new ConsoleAuditLogger()
        );
        lookupStrategy.setPermissionFactory(new DefaultPermissionFactory());
        return lookupStrategy;
    }

    @Bean
    public AclService aclService(LookupStrategy lookupStrategy,
                                AclCache aclCacheBean) {
        JdbcMutableAclService aclService = new JdbcMutableAclService(
                dataSource,
                lookupStrategy,
                aclCacheBean
        );
        aclService.setClassIdentityQuery("SELECT currval(pg_get_serial_sequence('acl_class', 'id'))");
        aclService.setSidIdentityQuery("SELECT currval(pg_get_serial_sequence('acl_sid', 'id'))");
        return aclService;
    }

    @Bean
    @Primary
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(AclService aclService) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        AclPermissionEvaluator permissionEvaluator = new AclPermissionEvaluator(aclService);
        expressionHandler.setPermissionEvaluator(permissionEvaluator);
        expressionHandler.setPermissionCacheOptimizer(new AclPermissionCacheOptimizer(aclService));
        return expressionHandler;
    }
}

