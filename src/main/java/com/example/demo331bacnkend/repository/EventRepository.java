package com.example.demo331bacnkend.repository;

import com.example.demo331bacnkend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EventRepository extends JpaRepository<Event, Long> {
}