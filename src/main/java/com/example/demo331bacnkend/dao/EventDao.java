package com.example.demo331bacnkend.dao;

import com.example.demo331bacnkend.entity.Event;
import org.springframework.data.domain.Page;

import java.util.List;

// EventDao.java
public interface EventDao {
    Integer getEventSize();
    Page<Event> getEvents(Integer pageSize, Integer page);
    Event getEvent(Long id);
    Event createEvent(Event event);
    Event save(Event event);
}

