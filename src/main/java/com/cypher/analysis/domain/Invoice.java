package com.cypher.analysis.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "invoice",
        indexes = {
                @Index(name = "idx_invoice_chave_nfe", columnList = "chave_nfe")
        }
)
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "raw_xml", columnDefinition = "TEXT", nullable = false)
    private String rawXml;

    @Column(name = "chave_nfe", length = 44, unique = true)
    private String nfeKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Invoice() {}

    private Invoice(String rawXml, String nfeKey) {
        this.rawXml   = rawXml;
        this.nfeKey = nfeKey;
        this.createdAt = Instant.now();
    }

    public static Invoice of(String xml, String nfeKey) {
        return new Invoice(xml, nfeKey);
    }

    public UUID getId()        { return id; }
    public String getRawXml()  { return rawXml; }
    public String getNfeKey()  { return nfeKey; }
}
