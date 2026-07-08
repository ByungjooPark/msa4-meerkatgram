package com.msa4meerkatgram.global.config.jpa;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryDSLConfig {
    // @PersistenceContext: JPA에서 DB와 상호작용을 하기 위한 객체 EntityManager를 Spring 컨텍스트에 자동으로 주입
    @PersistenceContext
    private EntityManager entityManager;
    // Entity의 영속성 관리를 담당하는 JPA의 핵심 인터페이스
    // CRUD작업, 쿼리 실행 등 DB와의 상호작용을 담당

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        // JPAQueryFactory: QueryDSL을 사용하기 위해 필요한 객체
        return new JPAQueryFactory(entityManager);
    }
}
