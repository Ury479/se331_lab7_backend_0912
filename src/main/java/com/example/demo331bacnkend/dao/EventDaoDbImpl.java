package com.example.demo331bacnkend.dao;

import com.example.demo331bacnkend.entity.Event;
import com.example.demo331bacnkend.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Profile("db")
public class EventDaoDbImpl implements EventDao {
    final EventRepository eventRepository;

    @Override
    public Integer getEventSize() {
        return Math.toIntExact(eventRepository.count());
    }

    @Override
    public Page<Event> getEvents(Integer pageSize, Integer page) {
        return eventRepository.findAll(PageRequest.of(page, pageSize));
    }

    @Override
    public Page<Event> getEvents(String title, Pageable page) {
        // 根据题意 1.7/1.8/1.9：可切换不同查询。此处选择“标题或描述或组织者 任一包含 + 忽略大小写”
        return eventRepository.findByTitleIgnoreCaseContainingOrDescriptionIgnoreCaseContainingOrOrganizerIgnoreCaseContaining(title, title, title, page);
    }

    // 1.7：标题且描述都包含（AND）
    @Override
    public Page<Event> getEventsAnd(String title, Pageable page) {
        return eventRepository.findByTitleContainingAndDescriptionContaining(title, title, page);
    }

    // 1.8：标题或描述或组织者任一包含（OR）
    @Override
    public Page<Event> getEventsOr(String title, Pageable page) {
        return eventRepository.findByTitleContainingOrDescriptionContainingOrOrganizerContaining(title, title, title, page);
    }

    @Override
    public Event getEvent(Long id) {
        return eventRepository.findById(id).orElse(null);
    }

    @Override
    public Event createEvent(Event event) {
        return eventRepository.save(event);
    }

    @Override
    public Event save(Event event) {
        return eventRepository.save(event);
    }
}

