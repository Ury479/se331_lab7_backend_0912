package com.example.demo331bacnkend.dao;

import com.example.demo331bacnkend.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


// EventDao.java
public interface EventDao {
    Integer getEventSize();
    Page<Event> getEvents(Integer pageSize, Integer page);
    Page<Event> getEvents(String title, Pageable page);
    // 1.7 AND 查询：标题且描述都包含
    Page<Event> getEventsAnd(String title, Pageable page);
    // 1.8 OR 查询：标题或描述或组织者任一包含
    Page<Event> getEventsOr(String title, Pageable page);
    Event getEvent(Long id);
    Event createEvent(Event event);
    Event save(Event event);
}

