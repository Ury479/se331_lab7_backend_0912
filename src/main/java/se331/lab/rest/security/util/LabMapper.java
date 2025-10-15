package se331.lab.rest.security.util;

import com.example.demo331bacnkend.entity.Organizer;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import se331.lab.rest.security.auth.AuthenticationResponse.OrganizerDTO;
import se331.lab.rest.security.auth.AuthenticationResponse.UserDTO;
import se331.lab.rest.security.user.User;

/**
 * 6.9: MapStruct Mapper - 避免堆栈溢出问题
 * 
 * MapStruct 在编译时生成映射代码，避免运行时反射和循环引用问题。
 * 使用 INSTANCE 单例模式，无需手动创建实例。
 */
@Mapper
public interface LabMapper {
    
    /**
     * 单例实例，由 MapStruct 在编译时生成
     */
    LabMapper INSTANCE = Mappers.getMapper(LabMapper.class);
    
    /**
     * 将 Organizer 实体转换为 OrganizerDTO
     * 
     * @param organizer Organizer 实体对象
     * @return OrganizerDTO 数据传输对象，如果 organizer 为 null 则返回 null
     */
    OrganizerDTO getOrganizerDTO(Organizer organizer);
    
    /**
     * 将 User 实体转换为 UserDTO
     * 
     * @param user User 实体对象
     * @return UserDTO 数据传输对象，如果 user 为 null 则返回 null
     */
    UserDTO getUserDTO(User user);
}

