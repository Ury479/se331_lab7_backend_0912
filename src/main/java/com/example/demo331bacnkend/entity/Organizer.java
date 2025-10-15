package com.example.demo331bacnkend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se331.lab.rest.security.user.User;

/** Organizer entity used for demo pagination and lookup by id. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Organizer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String organizationName;
    private String address;
    private String phone;
    private String website;
    private String image;  // 前端使用的字段名
    private String profileImage;  // 保留向后兼容
    
    // 6.4: 添加与 User 的一对一关联
    @OneToOne
    User user;
}
