package com.cypher.analysis.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NFeData {

    private String accessKey;
    private String number;
    private String series;
    private String operationNature;
    private String issuerCnpj;
    private String issuerLegalName;
    private String issuerState;
    private String recipientCnpj;
    private String recipientLegalName;
    private String recipientState;
    private BigDecimal totalAmount;
    private BigDecimal productsAmount;
    private BigDecimal freightAmount;
    private BigDecimal discountAmount;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private InvoiceStatus status;

    public NFeData() {
        this.totalAmount = BigDecimal.ZERO;
        this.productsAmount = BigDecimal.ZERO;
        this.freightAmount = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.status = InvoiceStatus.PENDING;
    }

    public String getAccessKey()                 { return accessKey; }
    public String getNumber()                    { return number; }
    public String getSeries()                    { return series; }
    public String getOperationNature()           { return operationNature; }
    public String getIssuerCnpj()                { return issuerCnpj; }
    public String getIssuerLegalName()           { return issuerLegalName; }
    public String getIssuerState()               { return issuerState; }
    public String getRecipientCnpj()             { return recipientCnpj; }
    public String getRecipientLegalName()        { return recipientLegalName; }
    public String getRecipientState()            { return recipientState; }
    public BigDecimal getTotalAmount()           { return totalAmount; }
    public BigDecimal getProductsAmount()        { return productsAmount; }
    public BigDecimal getFreightAmount()         { return freightAmount; }
    public BigDecimal getDiscountAmount()        { return discountAmount; }
    public LocalDate getIssueDate()              { return issueDate; }
    public LocalDate getDueDate()                { return dueDate; }
    public InvoiceStatus getStatus()             { return status; }

    public static Builder builder() {return new Builder();}

    public static class Builder {
        private final NFeData data = new NFeData();

        public Builder accessKey(String v)                { data.accessKey = v; return this; }
        public Builder number(String v)                   { data.number = v; return this; }
        public Builder series(String v)                   { data.series = v; return this; }
        public Builder operationNature(String v)          { data.operationNature = v; return this; }
        public Builder issuerCnpj(String v)               { data.issuerCnpj = v; return this; }
        public Builder issuerLegalName(String v)          { data.issuerLegalName = v; return this; }
        public Builder issuerState(String v)              { data.issuerState = v; return this; }
        public Builder recipientCnpj(String v)            { data.recipientCnpj = v; return this; }
        public Builder recipientLegalName(String v)       { data.recipientLegalName = v; return this; }
        public Builder recipientState(String v)           { data.recipientState = v; return this; }
        public Builder totalAmount(BigDecimal v)          { data.totalAmount = v; return this; }
        public Builder productsAmount(BigDecimal v)       { data.productsAmount = v; return this; }
        public Builder freightAmount(BigDecimal v)        { data.freightAmount = v; return this; }
        public Builder discountAmount(BigDecimal v)       { data.discountAmount = v; return this; }
        public Builder issueDate(LocalDate v)             { data.issueDate = v; return this; }
        public Builder dueDate(LocalDate v)               { data.dueDate = v; return this; }
        public Builder status(InvoiceStatus v)            { data.status = v; return this; }

        public NFeData build() {
            if (data.accessKey == null || data.accessKey.isBlank()) {
                throw new IllegalStateException("A chave de acesso é necessaria no NFeData.");
            }
            return data;
        }

        public NFeData buildUnsafe() {
            return data;
        }
    }
}
