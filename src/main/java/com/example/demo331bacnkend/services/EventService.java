package com.example.demo331bacnkend.services;

import com.example.demo331bacnkend.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface EventService {
    Integer getEventSize();
    Page<Event> getEvents(Integer pageSize, Integer page);
    Page<Event> getEvents(String title, Pageable pageable);
    // 1.7 AND：标题且描述都包含
    Page<Event> getEventsAnd(String title, Pageable pageable);
    // 1.8 OR：标题或描述或组织者任一包含
    Page<Event> getEventsOr(String title, Pageable pageable);
    Event getEvent(Long id);
    Event createEvent(Event event);

    Event save(Event event);
}
