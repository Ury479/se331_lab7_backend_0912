package com.example.demo331bacnkend.services;

import com.example.demo331bacnkend.entity.Action;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ActionService {
    List<Action> getAllActions();
    Page<Action> getActions(Pageable pageable);
    Action getAction(Long id);
    Action createAction(Action action);
    Action updateAction(Action action);
    void deleteAction(Long id);
}


