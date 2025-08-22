package com.momnect.userservice.command.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tbl_children")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Child {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 자녀의 고유 ID (PK)

    @Column(name = "user_id", nullable = false)
    private Long userId; // 부모(사용자)의 ID

    @Column(name = "nickname", length = 15)
    private String nickname; // 자녀 애칭

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate; // 자녀 생년월일
}
