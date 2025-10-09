package com.example.demo331bacnkend.services;

import com.example.demo331bacnkend.entity.Action;
import com.example.demo331bacnkend.repository.ActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActionServiceImpl implements ActionService {

    private final ActionRepository actionRepository;

    @Override
    public List<Action> getAllActions() {
        return actionRepository.findAll();
    }

    @Override
    public Page<Action> getActions(Pageable pageable) {
        return actionRepository.findAll(pageable);
    }

    @Override
    public Action getAction(Long id) {
        return actionRepository.findById(id).orElse(null);
    }

    @Override
    public Action createAction(Action action) {
        if (action.getTimestamp() == null) {
            action.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        return actionRepository.save(action);
    }

    @Override
    public Action updateAction(Action action) {
        return actionRepository.save(action);
    }

    @Override
    public void deleteAction(Long id) {
        actionRepository.deleteById(id);
    }
}


