package com.momnect.productservice.command.entity.hashtag;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_hashtag")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String name;
}
