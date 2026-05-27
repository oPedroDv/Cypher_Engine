package com.cypher.analysis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XmlStorageServiceTest {

    @TempDir
    private Path tempDir;

    private XmlStorageService service;

    @BeforeEach
    void setUp() {
        service = new XmlStorageService();
        ReflectionTestUtils.setField(service, "storageType", "local");
        ReflectionTestUtils.setField(service, "localBasePath", tempDir.toString());
    }

    @Test
    void storePersistsBase64XmlAndRetrieveReturnsBase64Payload() {
        UUID invoiceId = UUID.randomUUID();
        String xml = "<nfe><chNFe>123</chNFe></nfe>";
        String xmlBase64 = Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));

        String storagePath = service.store(xmlBase64, invoiceId, "123");

        assertThat(storagePath).isEqualTo("local://" + invoiceId + "/123.xml");
        assertThat(Path.of(tempDir.toString(), invoiceId.toString(), "123.xml"))
                .hasContent(xml);
        assertThat(service.retrieve(storagePath)).isEqualTo(xmlBase64);
    }

    @Test
    void storeTreatsInvalidBase64AsPlainUtf8Xml() {
        UUID invoiceId = UUID.randomUUID();
        String xml = "<nfe>plain</nfe>";

        String storagePath = service.store(xml, invoiceId, null);

        assertThat(storagePath).isEqualTo("local://" + invoiceId + "/raw.xml");
        assertThat(Path.of(tempDir.toString(), invoiceId.toString(), "raw.xml"))
                .hasContent(xml);
    }

    @Test
    void retrieveThrowsWhenLocalFileDoesNotExist() {
        assertThatThrownBy(() -> service.retrieve("local://missing/file.xml"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("XML não encontrado");
    }

    @Test
    void retrieveRejectsUnsupportedStorageType() {
        ReflectionTestUtils.setField(service, "storageType", "s3");

        assertThatThrownBy(() -> service.retrieve("s3://bucket/file.xml"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Storage type não suportado");
    }
}
