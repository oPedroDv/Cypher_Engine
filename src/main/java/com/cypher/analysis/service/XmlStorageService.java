package com.cypher.analysis.service;

import com.cypher.analysis.application.port.XmlStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

@Service
public class XmlStorageService implements XmlStoragePort {

    private static final Logger log = LoggerFactory.getLogger(XmlStorageService.class);

    @Value("${cypher.storage.type:local}")
    private String storageType;

    @Value("${cypher.storage.local-path:/tmp/cypher/xmls}")
    private String localBasePath;


    @Override
    public String store(String xmlBase64, UUID invoiceId, String nfeKey) {
        byte[] xmlBytes = decodeBase64(xmlBase64);
        String filename = nfeKey != null ? nfeKey + ".xml" : "raw.xml";
        String path     = invoiceId + "/" + filename;

        if ("local".equals(storageType)) {
            return storeLocal(xmlBytes, path);
        }

        log.warn("Storage type '{}' não implementado. Usando local.", storageType);
        return storeLocal(xmlBytes, path);
    }
    public String retrieve(String storagePath) {
        if ("local".equals(storageType)) {
            return retrieveLocal(storagePath);
        }
        throw new UnsupportedOperationException("Storage type não suportado: " + storageType);
    }

    private String storeLocal(byte[] xmlBytes, String path) {
        try {
            java.io.File file = new java.io.File(localBasePath + "/" + path);
            file.getParentFile().mkdirs();
            java.nio.file.Files.write(file.toPath(), xmlBytes);
            log.debug("XML armazenado localmente: {}", file.getAbsolutePath());
            return "local://" + path;
        } catch (java.io.IOException e) {
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
            log.debug("xmlBase64 não é Base64 válido — tratando como UTF-8 direto.");
            return xmlBase64.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
