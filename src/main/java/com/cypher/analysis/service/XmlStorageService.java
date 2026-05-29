package com.cypher.analysis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class XmlStorageService {

    @Value("${cypher.storage.type:local}")
    private String storageType;

    @Value("${cypher.storage.local-path:/tmp/cypher/xmls}")
    private String localBasePath;
    
    public String store(String xmlBase64, UUID invoiceId, String nfeKey) {
        byte[] xmlBytes = decodeBase64(xmlBase64);
        String filename = nfeKey != null ? nfeKey + ".xml" : "raw.xml";
        String path     = invoiceId + "/" + filename;

        if (!"local".equals(storageType)) {
            log.warn("Storage type '{}' não implementado. Usando local.", storageType);
        }

        return storeLocal(xmlBytes, path);
    }

    public String retrieve(String storagePath) {
        if (!"local".equals(storageType)) {
            throw new UnsupportedOperationException("Storage type não suportado: " + storageType);
        }
        return retrieveLocal(storagePath);
    }

    private String storeLocal(byte[] xmlBytes, String path) {
        try {
            File file = new File(localBasePath + "/" + path);
            file.getParentFile().mkdirs();
            Files.write(file.toPath(), xmlBytes);
            log.debug("XML armazenado localmente: {}", file.getAbsolutePath());
            return "local://" + path;
        } catch (IOException e) {
            log.error("Falha ao armazenar XML no caminho={}: {}", path, e.getMessage());
            throw new UncheckedIOException("Falha ao armazenar XML: " + path, e);
        }
    }

    private String retrieveLocal(String storagePath) {
        try {
            String cleanPath = storagePath.replace("local://", "");
            byte[] bytes = Files.readAllBytes(Path.of(localBasePath + "/" + cleanPath));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log.error("Falha ao recuperar XML storagePath={}: {}", storagePath, e.getMessage());
            throw new UncheckedIOException("XML não encontrado: " + storagePath, e);
        }
    }

    private byte[] decodeBase64(String xmlBase64) {
        try {
            return Base64.getDecoder().decode(xmlBase64);
        } catch (IllegalArgumentException e) {
            log.debug("xmlBase64 não é Base64 válido — tratando como UTF-8 direto.");
            return xmlBase64.getBytes(StandardCharsets.UTF_8);
        }
    }
}