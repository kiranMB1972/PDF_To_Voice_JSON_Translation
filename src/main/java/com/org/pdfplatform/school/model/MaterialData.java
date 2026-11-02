package com.org.pdfplatform.school.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sch_material_data")
public class MaterialData {

    @Id
    private Long id;

    @Lob
    @Column(name = "data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] data;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}
