package com.cypher.analysis.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "invoice",
        indexes = {
                @Index(name = "idx_invoice_chave_nfe", columnList = "chave_nfe"),
                @Index(name = "idx_invoice_issuer_cnpj", columnList = "issuer_cnpj")
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

    @Column(name = "issuer_cnpj", length = 14)
    private String issuerCnpj;

    @Column(name = "issuer_name", length = 255)
    private String issuerName;

    @Column(name = "recipient_cnpj", length = 14)
    private String recipientCnpj;

    @Column(name = "recipient_name", length = 255)
    private String recipientName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Invoice() {
    }

    private Invoice(
            String rawXml,
            String nfeKey,
            String issuerCnpj,
            String issuerName,
            String recipientCnpj,
            String recipientName
    ) {
        this.rawXml = rawXml;
        this.nfeKey = nfeKey;
        this.issuerCnpj = issuerCnpj;
        this.issuerName = issuerName;
        this.recipientCnpj = recipientCnpj;
        this.recipientName = recipientName;
        this.createdAt = Instant.now();
    }

    public static Invoice of(
            String xml,
            String nfeKey,
            String issuerCnpj,
            String issuerName,
            String recipientCnpj,
            String recipientName
    ) {
        return new Invoice(
                xml,
                nfeKey,
                issuerCnpj,
                issuerName,
                recipientCnpj,
                recipientName
        );
    }

    public static Invoice of(String xml, String nfeKey) {
        return new Invoice(xml, nfeKey, null, null, null, null);
    }

    public static Invoice from(String xml, NFeData nfeData) {
        return new Invoice(
                xml,
                nfeData.getAccessKey(),
                nfeData.getIssuerCnpj(),
                nfeData.getIssuerLegalName(),
                nfeData.getRecipientCnpj(),
                nfeData.getRecipientLegalName()
        );
    }

    public UUID getId() {
        return id;
    }

    public String getRawXml() {
        return rawXml;
    }

    public String getNfeKey() {
        return nfeKey;
    }

    public String getIssuerCnpj() {
        return issuerCnpj;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public String getRecipientCnpj() {
        return recipientCnpj;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
