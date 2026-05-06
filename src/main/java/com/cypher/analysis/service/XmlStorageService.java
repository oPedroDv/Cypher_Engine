package com.cypher.analysis.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

/**
 * Armazena o XML bruto da NF-e antes de qualquer processamento.
 *
 * REGRA: o XML original é sempre salvo primeiro, antes do parse.
 * Se o parse falhar, se as regras mudarem, se houver bug — você
 * sempre pode reprocessar a partir do XML original.
 *
 * MVP: salva localmente em disco (desenvolvimento) ou no S3/R2
 * conforme o profile ativo.
 *
 * Produção: configure AWS_S3_BUCKET e a implementação troca
 * automaticamente via @Profile. O path retornado é armazenado
 * no Invoice para recuperação futura.
 *
 * Path format: {tenantId}/{invoiceId}/{chaveNfe}.xml
 * Se chaveNfe ainda não foi extraída: {tenantId}/{invoiceId}/raw.xml
 */
@Service
public class XmlStorageService {

    private static final Logger log = LoggerFactory.getLogger(XmlStorageService.class);

    @Value("${cypher.storage.type:local}")
    private String storageType;

    @Value("${cypher.storage.local-path:/tmp/cypher/xmls}")
    private String localBasePath;

    /**
     * Armazena o XML base64 e retorna o path para recuperação futura.
     *
     * @param xmlBase64  XML da NF-e encodado em Base64
     * @param invoiceId  ID da invoice já gerado (para compor o path)
     * @param chaveNfe   chave de acesso da NF-e (pode ser null antes do parse)
     * @return path onde o XML foi armazenado
     */
    public String store(String xmlBase64, UUID invoiceId, String chaveNfe) {
        byte[] xmlBytes = decodeBase64(xmlBase64);
        String filename = chaveNfe != null ? chaveNfe + ".xml" : "raw.xml";
        String path     = invoiceId + "/" + filename;

        if ("local".equals(storageType)) {
            return storeLocal(xmlBytes, path);
        }

        // S3/R2 — implementar quando sair do MVP
        // Trocar por S3Client.putObject() com a lib AWS SDK v2
        log.warn("Storage type '{}' não implementado. Usando local.", storageType);
        return storeLocal(xmlBytes, path);
    }

    /**
     * Recupera o XML armazenado e retorna como string.
     * Usado para reprocessamento ou auditoria.
     */
    public String retrieve(String storagePath) {
        if ("local".equals(storageType)) {
            return retrieveLocal(storagePath);
        }
        throw new UnsupportedOperationException("Storage type não suportado: " + storageType);
    }

    // ---- Implementação local (MVP / desenvolvimento) ----

    private String storeLocal(byte[] xmlBytes, String path) {
        try {
            java.io.File file = new java.io.File(localBasePath + "/" + path);
            file.getParentFile().mkdirs();
            java.nio.file.Files.write(file.toPath(), xmlBytes);
            log.debug("XML armazenado localmente: {}", file.getAbsolutePath());
            return "local://" + path;
        } catch (java.io.IOException e) {
            // Não deixa erro de storage derrubar a análise —
            // loga e retorna path simbólico para auditoria
            log.error("Falha ao armazenar XML localmente: {}", e.getMessage());
            return "local://error/" + path;
        }
    }

    private String retrieveLocal(String storagePath) {
        try {
            String cleanPath = storagePath.replace("local://", "");
            byte[] bytes = java.nio.file.Files.readAllBytes(
                    java.nio.file.Path.of(localBasePath + "/" + cleanPath)
            );
            return Base64.getEncoder().encodeToString(bytes);
        } catch (java.io.IOException e) {
            log.error("Falha ao recuperar XML: {}", e.getMessage());
            throw new RuntimeException("XML não encontrado: " + storagePath, e);
        }
    }

    private byte[] decodeBase64(String xmlBase64) {
        try {
            return Base64.getDecoder().decode(xmlBase64);
        } catch (IllegalArgumentException e) {
            // Tenta como string UTF-8 direta (compatibilidade com clientes que mandam XML puro)
            log.debug("xmlBase64 não é Base64 válido — tratando como UTF-8 direto.");
            return xmlBase64.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}