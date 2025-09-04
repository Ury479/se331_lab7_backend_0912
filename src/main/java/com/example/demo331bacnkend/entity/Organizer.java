package com.example.demo331bacnkend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Organizer entity used for demo pagination and lookup by id. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organizer {
    private Long id;
    private String organizationName;
    private String address;
}
