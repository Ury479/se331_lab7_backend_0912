package com.example.demo331bacnkend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import com.example.demo331bacnkend.repository.EventRepository;
import com.example.demo331bacnkend.entity.Event;
import se331.lab.rest.security.user.Role;
import se331.lab.rest.security.user.User;
import se331.lab.rest.security.user.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.transaction.Transactional;

/**
 * Seed initial rows after application starts.
 */
@Component
@RequiredArgsConstructor
public class InitApp implements ApplicationListener<ApplicationReadyEvent> {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final com.example.demo331bacnkend.repository.OrganizerRepository organizerRepository;

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        addUsersAndOrganizers();  // 6.6: 合并用户和组织者创建，建立关联
        eventRepository.save(Event.builder()
                .category("Academic")
                .title("Midterm Exam")
                .description("A time for taking the exam")
                .location("CAMT Building")
                .date("3rd Sept")
                .time("3.00-4.00 pm.")
                .petAllowed(false)
                .organizer("CAMT")
                .build());

        eventRepository.save(Event.builder()
                .category("Academic")
                .title("Commencement Day")
                .description("A time for celebration")
                .location("CMU Convention hall")
                .date("21th Jan")
                .time("8.00am-4.00 pm.")
                .petAllowed(false)
                .organizer("CMU")
                .build());

        eventRepository.save(Event.builder()
                .category("Cultural")
                .title("Loy Krathong")
                .description("A time for Krathong")
                .location("Ping River")
                .date("21th Nov")
                .time("8.00-10.00 pm.")
                .petAllowed(false)
                .organizer("Chiang Mai")
                .build());

        eventRepository.save(Event.builder()
                .category("Cultural")
                .title("Songkran")
                .description("Let's Play Water")
                .location("Chiang Mai Moat")
                .date("13th April")
                .time("10.00am - 6.00 pm.")
                .petAllowed(true)
                .organizer("Chiang Mai Municipality")
                .build());
    }

    // 6.6: 创建用户和组织者，并建立双向关联
    private void addUsersAndOrganizers() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 创建用户
        User user1 = User.builder()
                .username("admin")
                .password(encoder.encode("admin"))
                .firstname("admin")
                .lastname("admin")
                .email("admin@admin.com")
                .enabled(true)
                .build();
        user1.getRoles().add(Role.ROLE_USER);
        user1.getRoles().add(Role.ROLE_ADMIN);
        
        User user2 = User.builder()
                .username("user")
                .password(encoder.encode("user"))
                .firstname("user")
                .lastname("user")
                .email("enabled@user.com")
                .enabled(true)
                .build();
        user2.getRoles().add(Role.ROLE_USER);
        
        User user3 = User.builder()
                .username("disableUser")
                .password(encoder.encode("disableUser"))
                .firstname("disableUser")
                .lastname("disableUser")
                .email("disableUser@user.com")
                .enabled(false)
                .build();
        user3.getRoles().add(Role.ROLE_USER);
        
        // 先保存用户
        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);
        user3 = userRepository.save(user3);
        
        // 创建组织者
        com.example.demo331bacnkend.entity.Organizer org1 = com.example.demo331bacnkend.entity.Organizer.builder()
                .organizationName("CAMT")
                .address("239 Huay Kaew Rd, Suthep, Muang, Chiang Mai")
                .build();
        
        com.example.demo331bacnkend.entity.Organizer org2 = com.example.demo331bacnkend.entity.Organizer.builder()
                .organizationName("CMU")
                .address("Chiang Mai University, 239 Huay Kaew Rd, Chiang Mai")
                .build();
        
        com.example.demo331bacnkend.entity.Organizer org3 = com.example.demo331bacnkend.entity.Organizer.builder()
                .organizationName("Chiang Mai Municipality")
                .address("Chiang Mai City Hall, Chang Khlan Rd, Chiang Mai")
                .build();
        
        // 6.6: 建立 User 和 Organizer 的双向关联
        org1.setUser(user1);
        user1.setOrganizer(org1);
        
        org2.setUser(user2);
        user2.setOrganizer(org2);
        
        org3.setUser(user3);
        user3.setOrganizer(org3);
        
        // 保存组织者（会级联保存关联）
        organizerRepository.save(org1);
        organizerRepository.save(org2);
        organizerRepository.save(org3);
        
        // 额外的组织者（没有关联用户）
        organizerRepository.save(com.example.demo331bacnkend.entity.Organizer.builder()
                .organizationName("Tourism Authority of Thailand")
                .address("1600 New Petchburi Rd, Makkasan, Ratchathewi, Bangkok")
                .build());

        organizerRepository.save(com.example.demo331bacnkend.entity.Organizer.builder()
                .organizationName("Department of Cultural Promotion")
                .address("Government Complex Building, Chaeng Wattana Rd, Bangkok")
                .build());
    }
}
