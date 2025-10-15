package se331.lab.rest.security.auth;

import com.example.demo331bacnkend.entity.Organizer;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se331.lab.rest.security.user.User;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {

  @JsonProperty("access_token")
  private String accessToken;
  @JsonProperty("refresh_token")
  private String refreshToken;
  
  // 6.3: 添加用户信息，登录后返回用户详情
  @JsonProperty("user")
  private UserDTO user;
  
  // 6.7: 添加 Organizer 信息，登录后返回组织者详情
  @JsonProperty("organizer")
  private OrganizerDTO organizer;
  
  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class UserDTO {
    private Integer id;
    private String username;
    private String firstname;
    private String lastname;
    private String email;
    private java.util.List<String> roles;
    
    public static UserDTO fromUser(User user) {
      return UserDTO.builder()
          .id(user.getId())
          .username(user.getUsername())
          .firstname(user.getFirstname())
          .lastname(user.getLastname())
          .email(user.getEmail())
          .roles(user.getRoles().stream()
              .map(Enum::name)
              .collect(java.util.stream.Collectors.toList()))
          .build();
    }
  }
  
  // 6.7: OrganizerDTO - 避免循环引用
  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class OrganizerDTO {
    private Long id;
    private String organizationName;
    private String address;
    private String phone;
    private String website;
    private String image;
    
    public static OrganizerDTO fromOrganizer(Organizer organizer) {
      if (organizer == null) {
        return null;
      }
      return OrganizerDTO.builder()
          .id(organizer.getId())
          .organizationName(organizer.getOrganizationName())
          .address(organizer.getAddress())
          .phone(organizer.getPhone())
          .website(organizer.getWebsite())
          .image(organizer.getImage())
          .build();
    }
  }
}
