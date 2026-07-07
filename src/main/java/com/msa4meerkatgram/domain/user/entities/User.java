package com.msa4meerkatgram.domain.user.entities;

import com.msa4meerkatgram.global.security.constant.ProviderPolicy;
import com.msa4meerkatgram.global.security.constant.RolePolicy;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Types;
import java.time.LocalDateTime;

@Entity // 해당 클래스가 JPA 엔티티임을 선언
@EntityListeners(AuditingEntityListener.class) // 엔티티의 이벤트 리스너 지정
@Table(name = "users") // 테이블명 맵핑
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL") // 엔티티의 조회 시 항상 특정 조건을 추가하도록 지정
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 자동 생성 전략 설정
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "nick", nullable = false, length = 20)
    private String nick;

    @Column(name = "provider", nullable = false, length = 10)
    @Enumerated(value = EnumType.STRING) // Enum을 어떤 데이터형식으로 저장할 건지 설정
    @JdbcTypeCode(Types.VARCHAR)
    private ProviderPolicy provider = ProviderPolicy.NONE;

    @Column(name = "role", nullable = false, length = 10)
    @Enumerated(value = EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    private RolePolicy role = RolePolicy.NORMAL;

    @Column(name = "profile", nullable = false, length = 100)
    private String profile;

    @Column(name = "refresh_token", nullable = true, length = 255)
    private String refreshToken;

    @CreatedDate // 생성 시 자동으로 시간 입력
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate // 수정 시 자동으로 시간 업데이트
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at", nullable = true)
    private LocalDateTime deletedAt;
}
