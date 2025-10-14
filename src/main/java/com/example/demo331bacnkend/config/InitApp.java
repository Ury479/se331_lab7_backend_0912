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
        addUser();
        addOrganizers();
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

    private void addUser() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // ✅ 确保 username 和 email 一致性
        User user1 = User.builder()
                .username("admin")  // username = 用户名
                .password(encoder.encode("admin"))
                .firstname("admin")
                .lastname("admin")
                .email("admin@admin.com")
                .enabled(true)
                .build();
        
        User user2 = User.builder()
                .username("user")  // username = 用户名
                .password(encoder.encode("user"))
                .firstname("user")
                .lastname("user")
                .email("enabled@user.com")
                .enabled(true)
                .build();
        
        User user3 = User.builder()
                .username("disableUser")  // username = 用户名
                .password(encoder.encode("disableUser"))
                .firstname("disableUser")
                .lastname("disableUser")
                .email("disableUser@user.com")
                .enabled(false)
                .build();
        
        user1.getRoles().add(Role.ROLE_USER);
        user1.getRoles().add(Role.ROLE_ADMIN);
        
        user2.getRoles().add(Role.ROLE_USER);
        user3.getRoles().add(Role.ROLE_USER);
        
        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);
    }

    private void addOrganizers() {
        organizerRepository.save(com.example.demo331bacnkend.entity.Organizer.builder()
                .organizationName("CAMT")
                .address("239 Huay Kaew Rd, Suthep, Muang, Chiang Mai")
                .build());

        organizerRepository.save(com.example.demo331bacnkend.entity.Organizer.builder()
                .organizationName("CMU")
                .address("Chiang Mai University, 239 Huay Kaew Rd, Chiang Mai")
                .build());

        organizerRepository.save(com.example.demo331bacnkend.entity.Organizer.builder()
                .organizationName("Chiang Mai Municipality")
                .address("Chiang Mai City Hall, Chang Khlan Rd, Chiang Mai")
                .build());

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
