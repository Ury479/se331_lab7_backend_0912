package com.example.demo331bacnkend.repository;

import com.example.demo331bacnkend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface EventRepository extends JpaRepository<Event, Long> {
    // 1) 按标题模糊匹配（包含）
    Page<Event> findByTitleContaining(String title, Pageable pageRequest);

    // 2) 标题或描述任一包含
    Page<Event> findByTitleContainingOrDescriptionContaining(String title, String description, Pageable pageRequest);

    // 3) 标题且描述同时包含
    Page<Event> findByTitleContainingAndDescriptionContaining(String title, String description, Pageable pageRequest);

    // 4) 标题或描述或组织者 任一包含（实体字段 organizer 为字符串）
    Page<Event> findByTitleContainingOrDescriptionContainingOrOrganizerContaining(String title, String description, String organizer, Pageable pageRequest);

    // 5) 忽略大小写：标题/描述/组织者 任一包含
    Page<Event> findByTitleIgnoreCaseContainingOrDescriptionIgnoreCaseContainingOrOrganizerIgnoreCaseContaining(String title, String description, String organizer, Pageable pageRequest);
}