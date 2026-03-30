package com.example.asset_management.model;

import com.example.asset_management.dto.enumerator.AttributeDataType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attribute")
public class Attribute {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attribute_id_gen")
    @SequenceGenerator(name = "attribute_id_gen", sequenceName = "attribute_id_seq", allocationSize = 1)
    @Column(name = "attribute_id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 50)
    private AttributeDataType dataType;

}