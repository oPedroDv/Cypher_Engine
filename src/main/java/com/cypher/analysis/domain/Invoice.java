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
    private String chaveNfe;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Invoice() {}

    private Invoice(String rawXml, String chaveNfe) {
        this.rawXml   = rawXml;
        this.chaveNfe = chaveNfe;
        this.createdAt = Instant.now();
    }

    public static Invoice of(String xml, String chaveNfe) {
        return new Invoice(xml, chaveNfe);
    }

    public UUID getId()        { return id; }
    public String getRawXml()  { return rawXml; }
    public String getChaveNfe(){ return chaveNfe; } 
}