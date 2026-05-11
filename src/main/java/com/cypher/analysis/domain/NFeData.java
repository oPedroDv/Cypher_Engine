package com.cypher.analysis.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NFeData {

    private String chaveAcesso;
    private String numero;
    private String serie;
    private String naturezaOperacao;
    private String cnpjEmitente;
    private String razaoSocialEmitente;
    private String ufEmitente;
    private String cnpjDestinatario;
    private String razaoSocialDestinatario;
    private String ufDestinatario;
    private BigDecimal valorTotal;
    private BigDecimal valorProdutos;
    private BigDecimal valorFrete;
    private BigDecimal valorDesconto;
    private LocalDate dataEmissao;
    private LocalDate dataVencimento;
    private InvoiceStatus status;

    public NFeData() {
        this.valorTotal = BigDecimal.ZERO;
        this.valorProdutos = BigDecimal.ZERO;
        this.valorFrete = BigDecimal.ZERO;
        this.valorDesconto = BigDecimal.ZERO;
        this.status = InvoiceStatus.PENDING;
    }

    public String getChaveAcesso()               { return chaveAcesso; }
    public String getNumero()                    { return numero; }
    public String getSerie()                     { return serie; }
    public String getNaturezaOperacao()          { return naturezaOperacao; }
    public String getCnpjEmitente()              { return cnpjEmitente; }
    public String getRazaoSocialEmitente()       { return razaoSocialEmitente; }
    public String getUfEmitente()                { return ufEmitente; }
    public String getCnpjDestinatario()          { return cnpjDestinatario; }
    public String getRazaoSocialDestinatario()   { return razaoSocialDestinatario; }
    public String getUfDestinatario()            { return ufDestinatario; }
    public BigDecimal getValorTotal()            { return valorTotal; }
    public BigDecimal getValorProdutos()         { return valorProdutos; }
    public BigDecimal getValorFrete()            { return valorFrete; }
    public BigDecimal getValorDesconto()         { return valorDesconto; }
    public LocalDate getDataEmissao()            { return dataEmissao; }
    public LocalDate getDataVencimento()         { return dataVencimento; }
    public InvoiceStatus getStatus()             { return status; }

    public static Builder builder() {return new Builder();}

    public static class Builder {
        private final NFeData data = new NFeData();

        public Builder chaveAcesso(String v)              { data.chaveAcesso = v; return this; }
        public Builder numero(String v)                   { data.numero = v; return this; }
        public Builder serie(String v)                    { data.serie = v; return this; }
        public Builder naturezaOperacao(String v)         { data.naturezaOperacao = v; return this; }
        public Builder cnpjEmitente(String v)             { data.cnpjEmitente = v; return this; }
        public Builder razaoSocialEmitente(String v)      { data.razaoSocialEmitente = v; return this; }
        public Builder ufEmitente(String v)               { data.ufEmitente = v; return this; }
        public Builder cnpjDestinatario(String v)         { data.cnpjDestinatario = v; return this; }
        public Builder razaoSocialDestinatario(String v)  { data.razaoSocialDestinatario = v; return this; }
        public Builder ufDestinatario(String v)           { data.ufDestinatario = v; return this; }
        public Builder valorTotal(BigDecimal v)           { data.valorTotal = v; return this; }
        public Builder valorProdutos(BigDecimal v)        { data.valorProdutos = v; return this; }
        public Builder valorFrete(BigDecimal v)           { data.valorFrete = v; return this; }
        public Builder valorDesconto(BigDecimal v)        { data.valorDesconto = v; return this; }
        public Builder dataEmissao(LocalDate v)           { data.dataEmissao = v; return this; }
        public Builder dataVencimento(LocalDate v)        { data.dataVencimento = v; return this; }
        public Builder status(InvoiceStatus v)            { data.status = v; return this; }

        public NFeData build() {
            if (data.chaveAcesso == null || data.chaveAcesso.isBlank()) {
                throw new IllegalStateException("A chave de acesso é necessaria no NFeData.");
            }
            return data;
        }
    public NFeData buildUnsafe() {
        return data;
        }
    }
}

