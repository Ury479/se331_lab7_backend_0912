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

    // 3) 可选：标题且描述同时包含（后续步骤可能需要）
    Page<Event> findByTitleContainingAndDescriptionContaining(String title, String description, Pageable pageRequest);
}