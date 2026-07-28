package com.cypher.analysis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class XmlStorageService {

    private static final long MAX_XML_SIZE_BYTES = 5 * 1024 * 1024L;

    @Value("${cypher.storage.type:local}")
    private String storageType;

    @Value("${cypher.storage.local-path:/tmp/cypher/xmls}")
    private String localBasePath;

    public String store(String xmlBase64, UUID invoiceId, String nfeKey) {
        byte[] xmlBytes = decodeBase64(xmlBase64);

        if (xmlBytes.length > MAX_XML_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "XML excede o tamanho máximo permitido (%d bytes)".formatted(MAX_XML_SIZE_BYTES)
            );
        }

        String safeFilename = sanitizeFilename(nfeKey != null ? nfeKey : "raw") + ".xml";
        String relativePath = invoiceId + "/" + safeFilename;

        if (!"local".equals(storageType)) {
            log.warn("Storage type '{}' não implementado. Usando local.", storageType);
        }

        return storeLocal(xmlBytes, relativePath);
    }

    public String retrieve(String storagePath) {
        if (!"local".equals(storageType)) {
            throw new UnsupportedOperationException("Storage type não suportado: " + storageType);
        }
        return retrieveLocal(storagePath);
    }

    private String storeLocal(byte[] xmlBytes, String relativePath) {
        try {
            Path base    = localBaseDirectory(true);
            Path target  = resolveAndValidate(base, relativePath);

            Files.createDirectories(target.getParent());

            Path temporary = Files.createTempFile(target.getParent(), ".tmp-", ".xml");
            try {
                Files.write(temporary, xmlBytes);
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }

            log.debug("XML armazenado localmente: {}", target);
            return "local://" + relativePath;

        } catch (IOException e) {
            log.error("Falha ao armazenar XML path={}: {}", relativePath, e.getMessage());
            throw new UncheckedIOException("Falha ao armazenar XML: " + relativePath, e);
        }
    }

    private String retrieveLocal(String storagePath) {
        try {
            String cleanPath = storagePath.replace("local://", "");
            Path base   = localBaseDirectory(false);
            Path target = resolveAndValidate(base, cleanPath);

            byte[] bytes = Files.readAllBytes(target);
            return Base64.getEncoder().encodeToString(bytes);

        } catch (IOException e) {
            log.error("Falha ao recuperar XML storagePath={}: {}", storagePath, e.getMessage());
            throw new UncheckedIOException("XML não encontrado: " + storagePath, e);
        }
    }

    private Path resolveAndValidate(Path base, String relativePath) throws IOException {
        Path resolved;
        try {
            resolved = base.resolve(relativePath).normalize();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Caminho inválido: " + relativePath, e);
        }

        if (!resolved.startsWith(base)) {
            log.error("Tentativa de path traversal bloqueada: base={} path={}", base, relativePath);
            throw new IllegalArgumentException("Caminho fora do diretório permitido");
        }

        return resolved;
    }

    private Path localBaseDirectory(boolean create) throws IOException {
        Path base = Path.of(localBasePath).toAbsolutePath().normalize();
        if (create) {
            Files.createDirectories(base);
        }
        return base.toRealPath();
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
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
