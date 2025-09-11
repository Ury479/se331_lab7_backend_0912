package com.example.demo331bacnkend.dao;

import com.example.demo331bacnkend.entity.Organizer;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@Profile("manual")
public class OrganizerDaoImpl implements OrganizerDao {

    private List<Organizer> organizerList;

    @PostConstruct
    public void init() {
        organizerList = new ArrayList<>();
        organizerList.add(Organizer.builder().id(1L).organizationName("Meow Shelter").address("12 Cat St, Meow Town").build());
        organizerList.add(Organizer.builder().id(2L).organizationName("Green Thumb Club").address("45 Flora Ave, Flora City").build());
        organizerList.add(Organizer.builder().id(3L).organizationName("Ocean Care").address("30 Shore Rd, Playa Del Carmen").build());
        organizerList.add(Organizer.builder().id(4L).organizationName("Doggo Rescue").address("8 Bark Blvd, Woof Town").build());
        organizerList.add(Organizer.builder().id(5L).organizationName("Food Bank").address("77 Tin Rd, Tin City").build());
        organizerList.add(Organizer.builder().id(6L).organizationName("Road Cleaners").address("Hwy 50 Service, Exit 3").build());
    }

    @Override
    public Integer getOrganizerSize() {
        return organizerList.size();
    }

    @Override
    public List<Organizer> getOrganizers(Integer pageSize, Integer page) {
        pageSize = pageSize == null ? organizerList.size() : pageSize;
        page = page == null ? 1 : page;
        int firstIndex = (page - 1) * pageSize;
        return organizerList.subList(firstIndex, Math.min(firstIndex + pageSize, organizerList.size()));
    }

    @Override
    public Organizer getOrganizer(Long id) {
        return organizerList.stream()
                .filter(o -> o.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Organizer save(Organizer organizer) {
        if (organizer.getId() == null) {
            long nextId = organizerList.stream()
                    .map(Organizer::getId)
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L) + 1;
            organizer.setId(nextId);
        } else {
            for (int i = 0; i < organizerList.size(); i++) {
                if (organizerList.get(i).getId().equals(organizer.getId())) {
                    organizerList.set(i, organizer);
                    return organizer;
                }
            }
        }
        organizerList.add(organizer);
        return organizer;
    }
}
