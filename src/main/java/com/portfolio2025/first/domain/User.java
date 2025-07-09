package com.portfolio2025.first.domain;

import com.portfolio2025.first.domain.vo.Money;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "users")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 기본 생성자
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "user_id", nullable = false, length = 30)
    private String userId;

    private String location;

    @Column(name = "phone_number")
    private String phoneNumber;
    private String email;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Portfolio> portfolios = new ArrayList<>(); // 양방향으로 처리하기

    @Builder
    private User(String name, String location, String phoneNumber, String email, String userId) {
        if (name == null || userId == null) {
            throw new IllegalArgumentException("이름과 아이디는 필수입니다.");
        }

        this.name = name;
        this.location = location;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static User createUser(String name, String location, String phoneNumber, String email, String userId) {
        return User.builder()
                .name(name)
                .location(location)
                .phoneNumber(phoneNumber)
                .email(email)
                .userId(userId)
                .build();
    }

    public void updateContact(String phoneNumber, String email) {
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateLocation(String location) {
        this.location = location;
        this.updatedAt = LocalDateTime.now();
    }

    /** 양방향 편의 메서드 **/
    public void addPortfolio(Portfolio portfolio) {
        portfolios.add(portfolio);
    }
}

