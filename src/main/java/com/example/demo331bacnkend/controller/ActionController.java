package com.example.demo331bacnkend.controller;

import com.example.demo331bacnkend.entity.Action;
import com.example.demo331bacnkend.services.ActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/actions")
@CrossOrigin(origins = "*")
public class ActionController {
    
    @Autowired
    private ActionService actionService;
    
    @GetMapping
    public ResponseEntity<List<Action>> getAllActions(
            @RequestParam(value = "_limit", required = false) Integer perPage,
            @RequestParam(value = "_page", required = false) Integer page) {
        
        if (perPage != null && page != null) {
            Page<Action> pageResult = actionService.getActions(PageRequest.of(page - 1, perPage));
            return ResponseEntity.ok()
                    .header("x-total-count", String.valueOf(pageResult.getTotalElements()))
                    .body(pageResult.getContent());
        }
        
        List<Action> actions = actionService.getAllActions();
        return ResponseEntity.ok(actions);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Action> getAction(@PathVariable Long id) {
        Action action = actionService.getAction(id);
        if (action != null) {
            return ResponseEntity.ok(action);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found");
    }
    
    @PostMapping
    public ResponseEntity<Action> createAction(@RequestBody Action action) {
        Action createdAction = actionService.createAction(action);
        return new ResponseEntity<>(createdAction, HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Action> updateAction(@PathVariable Long id, @RequestBody Action action) {
        action.setId(id);
        Action updatedAction = actionService.updateAction(action);
        return ResponseEntity.ok(updatedAction);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAction(@PathVariable Long id) {
        actionService.deleteAction(id);
        return ResponseEntity.noContent().build();
    }
}


