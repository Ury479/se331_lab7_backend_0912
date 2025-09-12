package com.example.demo331bacnkend.dao;

import com.example.demo331bacnkend.entity.Organizer;
import com.example.demo331bacnkend.repository.OrganizerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Profile("db")
public class OrganizerDaoDbImpl implements OrganizerDao {

    private final OrganizerRepository organizerRepository;

    @Override
    public Integer getOrganizerSize() {
        return Math.toIntExact(organizerRepository.count());
    }

    @Override
    public List<Organizer> getOrganizers(Integer pageSize, Integer page) {
        // 简化：一次性返回全部，前端已有分页参数时可在 service/dao 扩展 Page 版本
        return organizerRepository.findAll();
    }

    @Override
    public Organizer getOrganizer(Long id) {
        return organizerRepository.findById(id).orElse(null);
    }

    @Override
    public Organizer save(Organizer organizer) {
        return organizerRepository.save(organizer);
    }
}



