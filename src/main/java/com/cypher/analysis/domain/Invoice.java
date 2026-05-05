package com.cypher.analysis.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoice")
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

    protected Invoice() {
        // JPA only
    }

    private Invoice(String rawXml, String chaveNfe) {
        this.rawXml = rawXml;
        this.chaveNfe = chaveNfe;
        this.createdAt = Instant.now();
    }

    public static Invoice of(String xml) {
        // aqui você deveria extrair a chave do XML
        String chave = extractChave(xml);

        return new Invoice(xml, chave);
    }

    private static String extractChave(String xml) {
        // placeholder - você VAI precisar parsear isso depois
        return UUID.randomUUID().toString().replace("-", "").substring(0, 44);
    }

    public UUID getId() {
        return id;
    }

    public String getRawXml() {
        return rawXml;
    }
}