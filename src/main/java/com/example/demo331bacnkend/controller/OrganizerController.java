package com.example.demo331bacnkend.controller;

import com.example.demo331bacnkend.entity.Organizer;
import com.example.demo331bacnkend.services.OrganizerService;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** REST endpoints for organizers: pagination and get-by-id. */
@RestController
@RequestMapping("/organizers")
public class OrganizerController {

    private final OrganizerService organizerService;

    public OrganizerController(OrganizerService organizerService) {
        this.organizerService = organizerService;
    }

    // 负责页面分页功能
    // GET /organizers?_limit=...&_page=...
    @GetMapping
    public ResponseEntity<List<Organizer>> list(
            @RequestParam(value = "_limit", required = false) Integer perPage,
            @RequestParam(value = "_page",  required = false) Integer page) {
        
        // 设置默认值以避免 NullPointerException
        Integer pageNumber = (page != null) ? page : 1;
        Integer pageSize = (perPage != null) ? perPage : 10;

        int total = organizerService.getOrganizerSize();
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-total-count", String.valueOf(total));

        try {
            List<Organizer> items = organizerService.getOrganizers(pageSize, pageNumber);
            return new ResponseEntity<>(items, headers, HttpStatus.OK);
        } catch (IndexOutOfBoundsException ex) {
            // follow the same lab behavior as events: return empty with 200
            return new ResponseEntity<>(List.of(), headers, HttpStatus.OK);
        }
    }

    // GET /organizers/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Organizer> getOne(@PathVariable Long id) {
        Organizer o = organizerService.getOrganizer(id);
        if (o != null) return ResponseEntity.ok(o);
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The given id is not found");
    }

    // POST /organizers - 创建新的组织者
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Organizer> create(@RequestBody Organizer org) {
        try {
            // 确保 id 为 null，让数据库自动生成
            org.setId(null);
            
            // 基本验证 - organizationName 和 address 至少有一个
            boolean hasName = org.getOrganizationName() != null && !org.getOrganizationName().isBlank();
            boolean hasAddress = org.getAddress() != null && !org.getAddress().isBlank();
            
            if (!hasName && !hasAddress) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least organizationName or address is required");
            }
            
            Organizer saved = organizerService.save(org);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to create organizer: " + ex.getMessage());
        }
    }

    // PUT /organizers/{id} - 更新组织者信息
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Organizer> update(@PathVariable Long id, @RequestBody Organizer org) {
        Organizer existing = organizerService.getOrganizer(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organizer with id " + id + " not found");
        }
        
        // 更新字段
        org.setId(id); // 确保使用正确的 ID
        
        Organizer updated = organizerService.save(org);
        return ResponseEntity.ok(updated);
    }

    // DELETE /organizers/{id} - 删除组织者
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Organizer existing = organizerService.getOrganizer(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organizer with id " + id + " not found");
        }
        
        organizerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
