package com.cypher.analysis.service;

import com.cypher.analysis.domain.InvoiceStatus;

public interface SefazClient {
    InvoiceStatus consultStats(String accessKey);
}
