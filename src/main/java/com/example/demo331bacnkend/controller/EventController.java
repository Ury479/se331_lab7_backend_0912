package com.example.demo331bacnkend.controller;

import com.example.demo331bacnkend.entity.Event;
import com.example.demo331bacnkend.services.EventService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1") // unify API prefix
public class EventController {

    // Prefer constructor injection; if you use Lombok, you can add @RequiredArgsConstructor on class.
    private final EventService eventService;
    public EventController(com.example.demo331bacnkend.services.EventService eventService) {
        this.eventService = eventService;
    }

    /** GET /events with pagination: _limit & _page 实现分页功能*/
    @GetMapping("/events") // full path: /api/v1/events
    public ResponseEntity<List<com.example.demo331bacnkend.entity.Event>> getEventLists(
            @RequestParam(value = "_limit", required = false) Integer perPage,
            @RequestParam(value = "_page",  required = false) Integer page) {
        // 设置默认值以避免 NullPointerException
        Integer pageNumber = (page != null) ? page : 1;
        Integer pageSize = (perPage != null) ? perPage : 10;
        
        Page<com.example.demo331bacnkend.entity.Event> pageOutput = eventService.getEvents(pageSize, pageNumber);
        HttpHeaders responseHeader = new HttpHeaders();
        // Expose total count to frontend for pagination
        responseHeader.set("x-total-count",String.valueOf(pageOutput.getTotalElements()));
        return  new ResponseEntity<>(pageOutput.getContent(), responseHeader, HttpStatus.OK);
    }

    /** GET /events/{id} - fetch single event by id */
    @GetMapping("/events/{id}") // full path: /api/v1/events/{id}
    public ResponseEntity<com.example.demo331bacnkend.entity.Event> getEvent(@PathVariable("id") Long id) {
        com.example.demo331bacnkend.entity.Event output = eventService.getEvent(id);
        // lab7 中的要求
        if (output != null) {
            return ResponseEntity.ok(output);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The given id is not found");
    }

    /** POST /events - create new event */
    @PostMapping("/events") // full path: /api/v1/events
    public ResponseEntity<com.example.demo331bacnkend.entity.Event> createEvent(@RequestBody com.example.demo331bacnkend.entity.Event event) {
        try {
            // 确保 id 为 null，让数据库自动生成
            event.setId(null);
            
            // Basic validation
            if (event.getTitle() == null || event.getTitle().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event title is required");
            }
            if (event.getDescription() == null || event.getDescription().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event description is required");
            }
            if (event.getLocation() == null || event.getLocation().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event location is required");
            }
            if (event.getDate() == null || event.getDate().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event date is required");
            }
            
            com.example.demo331bacnkend.entity.Event createdEvent = eventService.createEvent(event);
            return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
        } catch (ResponseStatusException rse) {
            throw rse; // Re-throw validation errors
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to create event: " + ex.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> updateEvent(@RequestBody com.example.demo331bacnkend.entity.Event event) {
        com.example.demo331bacnkend.entity.Event output = eventService.save(event);
        return ResponseEntity.ok(output);
    }
}
