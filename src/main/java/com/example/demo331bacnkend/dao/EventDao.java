package com.example.demo331bacnkend.dao;

import com.example.demo331bacnkend.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


// EventDao.java
public interface EventDao {
    Integer getEventSize();
    Page<Event> getEvents(Integer pageSize, Integer page);
    Page<Event> getEvents(String title, Pageable page);
    Event getEvent(Long id);
    Event createEvent(Event event);
    Event save(Event event);
}

