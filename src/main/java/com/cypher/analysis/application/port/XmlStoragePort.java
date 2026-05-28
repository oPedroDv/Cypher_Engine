package com.cypher.analysis.application.port;

import java.util.UUID;

public interface XmlStoragePort {

    String store(String xmlBase64, UUID invoiceId, String nfeKey);
}
