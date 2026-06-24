package com.cypher.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestSizeLimitFilterTest {

    @Test
    void rejectsOversizedRequestBeforeDispatch() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(100);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/analyses");
        request.setContent(new byte[101]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("PAYLOAD_TOO_LARGE");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void rejectsOversizedChunkedRequestWhileReadingBoundedBody() throws Exception {
        RequestSizeLimitFilter filter = new RequestSizeLimitFilter(100);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/analyses") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContent(new byte[101]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(chain.getRequest()).isNull();
    }
}
