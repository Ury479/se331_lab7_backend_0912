# MapStruct 实现说明 (6.9)

## 📋 问题背景

### ❌ 原始问题：堆栈溢出（StackOverflowError）

在返回用户和组织者信息时，如果直接使用手动转换方法，可能会遇到：

1. **循环引用问题**：
   ```
   User → Organizer → User → Organizer → ... (无限循环)
   ```

2. **手动维护成本高**：
   - 每次实体字段变化都要手动更新转换方法
   - 容易出错
   - 代码重复

3. **性能问题**：
   - 运行时反射
   - 大量对象创建

---

## ✅ 解决方案：使用 MapStruct

### 什么是 MapStruct？

**MapStruct** 是一个 Java 注解处理器，用于生成类型安全的 Bean 映射代码。

**核心优势**：
- ✅ **编译时生成代码** - 无运行时开销
- ✅ **类型安全** - 编译时检查
- ✅ **避免循环引用** - 显式控制映射关系
- ✅ **自动维护** - 字段变化时自动同步
- ✅ **高性能** - 直接调用 getter/setter，无反射

---

## 🔧 实现步骤

### 步骤 1: 添加 MapStruct 依赖

**文件**: `pom.xml`

```xml
<properties>
    <java.version>17</java.version>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
</properties>

<dependencies>
    <!-- MapStruct for DTO mapping (6.9: 避免堆栈溢出) -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${mapstruct.version}</version>
    </dependency>
</dependencies>
```

---

### 步骤 2: 配置注解处理器

**文件**: `pom.xml` - `maven-compiler-plugin`

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <!-- Lombok 注解处理器 -->
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>1.18.34</version>
            </path>
            
            <!-- MapStruct 注解处理器 (6.9) -->
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </path>
            
            <!-- Lombok + MapStruct 兼容 -->
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok-mapstruct-binding</artifactId>
                <version>0.2.0</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

**为什么需要 `lombok-mapstruct-binding`？**
- Lombok 和 MapStruct 都是编译时注解处理器
- `lombok-mapstruct-binding` 确保两者协同工作
- 让 MapStruct 能看到 Lombok 生成的 getter/setter

---

### 步骤 3: 创建 LabMapper 接口

**文件**: `src/main/java/se331/lab/rest/security/util/LabMapper.java`

```java
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
```

**关键点**：
- `@Mapper` - 标记为 MapStruct mapper
- `INSTANCE` - 单例模式，由 `Mappers.getMapper()` 创建
- 方法声明 - 只需声明接口，实现由 MapStruct 自动生成

---

### 步骤 4: 修改 AuthenticationService 使用 LabMapper

**文件**: `src/main/java/se331/lab/rest/security/auth/AuthenticationService.java`

#### 修改前（手动转换）：

```java
// ❌ 手动转换，容易出错
return AuthenticationResponse.builder()
    .accessToken(jwtToken)
    .refreshToken(refreshToken)
    .user(AuthenticationResponse.UserDTO.fromUser(user))
    .organizer(AuthenticationResponse.OrganizerDTO.fromOrganizer(user.getOrganizer()))
    .build();
```

#### 修改后（使用 MapStruct）：

```java
// ✅ 使用 MapStruct 自动转换
import se331.lab.rest.security.util.LabMapper;

return AuthenticationResponse.builder()
    .accessToken(jwtToken)
    .refreshToken(refreshToken)
    .user(AuthenticationResponse.UserDTO.fromUser(user))
    .organizer(LabMapper.INSTANCE.getOrganizerDTO(user.getOrganizer()))  // 6.9: 使用 MapStruct
    .build();
```

---

## 🏗️ MapStruct 生成的实现类

**编译后自动生成**: `target/generated-sources/annotations/se331/lab/rest/security/util/LabMapperImpl.java`

```java
package se331.lab.rest.security.util;

import com.example.demo331bacnkend.entity.Organizer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import se331.lab.rest.security.auth.AuthenticationResponse;
import se331.lab.rest.security.user.Role;
import se331.lab.rest.security.user.User;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-15T15:38:22+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17"
)
public class LabMapperImpl implements LabMapper {

    @Override
    public AuthenticationResponse.OrganizerDTO getOrganizerDTO(Organizer organizer) {
        if ( organizer == null ) {
            return null;  // ✅ 自动 null 检查
        }

        AuthenticationResponse.OrganizerDTO.OrganizerDTOBuilder organizerDTO = 
            AuthenticationResponse.OrganizerDTO.builder();

        // ✅ 自动字段映射
        organizerDTO.address( organizer.getAddress() );
        organizerDTO.id( organizer.getId() );
        organizerDTO.image( organizer.getImage() );
        organizerDTO.organizationName( organizer.getOrganizationName() );
        organizerDTO.phone( organizer.getPhone() );
        organizerDTO.website( organizer.getWebsite() );

        return organizerDTO.build();  // ✅ 使用 Builder 模式
    }

    @Override
    public AuthenticationResponse.UserDTO getUserDTO(User user) {
        if ( user == null ) {
            return null;  // ✅ 自动 null 检查
        }

        AuthenticationResponse.UserDTO.UserDTOBuilder userDTO = 
            AuthenticationResponse.UserDTO.builder();

        // ✅ 自动字段映射
        userDTO.email( user.getEmail() );
        userDTO.firstname( user.getFirstname() );
        userDTO.id( user.getId() );
        userDTO.lastname( user.getLastname() );
        userDTO.roles( roleListToStringList( user.getRoles() ) );  // ✅ 自动生成类型转换
        userDTO.username( user.getUsername() );

        return userDTO.build();
    }

    // ✅ 自动生成辅助方法
    protected List<String> roleListToStringList(List<Role> list) {
        if ( list == null ) {
            return null;
        }

        List<String> list1 = new ArrayList<String>( list.size() );
        for ( Role role : list ) {
            list1.add( role.name() );
        }

        return list1;
    }
}
```

**MapStruct 自动处理**：
- ✅ Null 检查
- ✅ 字段名称匹配
- ✅ 类型转换（如 `List<Role>` → `List<String>`）
- ✅ Builder 模式支持
- ✅ 循环引用避免（只映射需要的字段）

---

## 🧪 测试结果

### 测试 1: admin 用户登录

**请求**：
```bash
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

**响应**：
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 1,
    "username": "admin",
    "firstname": "admin",
    "lastname": "admin",
    "email": "admin@admin.com",
    "roles": ["ROLE_USER", "ROLE_ADMIN"]
  },
  "organizer": {
    "id": 1,
    "organizationName": "CAMT",
    "address": "239 Huay Kaew Rd, Suthep, Muang, Chiang Mai",
    "phone": null,
    "website": null,
    "image": null
  }
}
```

**✅ 验证成功**：
- ✅ 无堆栈溢出错误
- ✅ 正确返回用户和组织者信息
- ✅ 无循环引用
- ✅ 性能良好

---

## 📊 MapStruct vs 手动转换对比

| 特性 | 手动转换 | MapStruct |
|------|----------|-----------|
| **实现方式** | 手动编写转换代码 | 编译时自动生成 |
| **类型安全** | ❌ 运行时才能发现错误 | ✅ 编译时检查 |
| **性能** | ❌ 可能使用反射 | ✅ 直接调用 getter/setter |
| **维护成本** | ❌ 字段变化需手动同步 | ✅ 自动同步 |
| **循环引用** | ❌ 需手动处理 | ✅ 自动避免 |
| **Null 安全** | ❌ 需手动检查 | ✅ 自动生成检查 |
| **代码量** | ❌ 大量重复代码 | ✅ 只需接口声明 |

---

## 🎯 MapStruct 最佳实践

### 1. 使用单例模式

```java
@Mapper
public interface LabMapper {
    LabMapper INSTANCE = Mappers.getMapper(LabMapper.class);
    // ...
}

// 使用
LabMapper.INSTANCE.getOrganizerDTO(organizer);
```

### 2. Spring 集成（可选）

```java
@Mapper(componentModel = "spring")
public interface LabMapper {
    // 不需要 INSTANCE
}

// Spring 自动注入
@Service
public class SomeService {
    private final LabMapper labMapper;
    
    public SomeService(LabMapper labMapper) {
        this.labMapper = labMapper;
    }
}
```

### 3. 自定义映射

```java
@Mapper
public interface LabMapper {
    
    @Mapping(source = "organizationName", target = "name")  // 字段名不同
    @Mapping(target = "createdAt", ignore = true)  // 忽略某些字段
    OrganizerDTO toDTO(Organizer organizer);
}
```

### 4. 集合映射

```java
@Mapper
public interface LabMapper {
    
    List<OrganizerDTO> toOrganizerDTOList(List<Organizer> organizers);
}
```

---

## 🔍 常见问题

### Q1: 为什么生成的代码在 target 目录？

**A**: MapStruct 是编译时注解处理器，生成的代码放在：
```
target/generated-sources/annotations/
```

这些代码会被自动编译并打包到最终的 JAR 中。

---

### Q2: 如何查看生成的代码？

**A**: 
1. 运行 `mvn compile`
2. 查看 `target/generated-sources/annotations/` 目录
3. 在 IDE 中可能需要标记为 "Generated Sources Root"

---

### Q3: 为什么需要 lombok-mapstruct-binding？

**A**: 
- Lombok 在编译时生成 getter/setter
- MapStruct 也在编译时运行
- `lombok-mapstruct-binding` 确保 MapStruct 能看到 Lombok 生成的代码
- **处理顺序**: Lombok → MapStruct

---

### Q4: MapStruct 如何避免循环引用？

**A**: 
```java
// DTO 中不包含反向引用
public class OrganizerDTO {
    private Long id;
    private String organizationName;
    // ❌ 没有 User user; 字段
}

public class UserDTO {
    private Integer id;
    private String username;
    // ❌ 没有 Organizer organizer; 字段
}
```

MapStruct 只映射 DTO 中声明的字段，不会递归映射整个对象图。

---

### Q5: 编译时看到 MapStruct 警告怎么办？

**A**: 
```
warning: Unmapped target property: "someField"
```

**解决方案**：
1. 添加对应字段到 DTO
2. 或使用 `@Mapping(target = "someField", ignore = true)` 忽略

---

## 📝 修改的文件总结

| 文件 | 修改内容 |
|------|----------|
| `pom.xml` | 添加 MapStruct 依赖和注解处理器配置 |
| `LabMapper.java` (新建) | 定义映射接口 |
| `AuthenticationService.java` | 使用 `LabMapper.INSTANCE.getOrganizerDTO()` |

---

## ✅ 实现验证

### 编译验证

```bash
mvn clean compile
```

**期望输出**：
```
[INFO] --- compiler:3.14.0:compile (default-compile) ---
[INFO] Compiling 54 source files
[INFO] BUILD SUCCESS
```

### 运行时验证

```bash
curl -X POST http://localhost:8080/api/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

**期望**：
- ✅ 返回 200 OK
- ✅ 包含 `user` 和 `organizer` 字段
- ✅ 无 `StackOverflowError`

### 生成文件验证

```bash
ls target/generated-sources/annotations/se331/lab/rest/security/util/
```

**期望输出**：
```
LabMapperImpl.java
```

---

## 🎉 总结

### 问题解决

- ❌ **问题**: 手动转换导致堆栈溢出和维护困难
- ✅ **解决**: 使用 MapStruct 自动生成类型安全的转换代码

### 核心优势

1. ✅ **编译时生成** - 无运行时开销
2. ✅ **类型安全** - 编译时检查
3. ✅ **自动维护** - 字段变化自动同步
4. ✅ **避免循环引用** - 显式控制映射
5. ✅ **高性能** - 直接 getter/setter 调用

### 最佳实践

- 使用 `@Mapper` 注解标记接口
- 使用 `INSTANCE` 单例模式
- DTO 中只包含需要的字段（避免循环引用）
- 配置 Lombok + MapStruct 兼容

---

**📅 实现日期**: 2025-10-15  
**✅ 状态**: 完成并测试通过  
**📦 MapStruct 版本**: 1.5.5.Final

