/**
 * @file CacheConfig.java
 * @description 애플리케이션의 캐시 설정을 구성합니다.
 *              Caffeine 캐시 라이브러리를 사용하여 성능을 최적화합니다.
 */
package com.example.javaversion.config;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
public class CacheConfig {

    @Value("${dart.api.cache.companyProfiles.ttl:3600}")
    private int companyProfilesTtl;

    @Value("${dart.api.cache.companyProfiles.maxSize:1000}")
    private int companyProfilesMaxSize;

    @Value("${dart.api.cache.disclosureSearch.ttl:3600}")
    private int disclosureSearchTtl;

    @Value("${dart.api.cache.disclosureSearch.maxSize:500}")
    private int disclosureSearchMaxSize;

    @Value("${dart.api.cache.dartCorpCodes.ttl:86400}")
    private int dartCorpCodesTtl;

    @Value("${dart.api.cache.dartCorpCodes.maxSize:10000}")
    private int dartCorpCodesMaxSize;

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        CaffeineCache companyProfilesCache = new CaffeineCache("companyProfiles", 
                Caffeine.newBuilder()
                    .expireAfterWrite(companyProfilesTtl, TimeUnit.SECONDS)
                    .maximumSize(companyProfilesMaxSize)
                    .build());

        CaffeineCache disclosureSearchCache = new CaffeineCache("disclosureSearch", 
                Caffeine.newBuilder()
                    .expireAfterWrite(disclosureSearchTtl, TimeUnit.SECONDS)
                    .maximumSize(disclosureSearchMaxSize)
                    .build());

        CaffeineCache dartCorpCodesCache = new CaffeineCache("dartCorpCodes", 
                Caffeine.newBuilder()
                    .expireAfterWrite(dartCorpCodesTtl, TimeUnit.SECONDS) 
                    .maximumSize(dartCorpCodesMaxSize)
                    .build());

        cacheManager.setCaches(Arrays.asList(
                companyProfilesCache, 
                disclosureSearchCache,
                dartCorpCodesCache
        ));

        return cacheManager;
    }
} 
