package com.cypher.shared.web;

import com.cypher.shared.exception.InvalidNFeException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ProbeController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void mapsIllegalArgumentToBadRequestWithItsMessage() throws Exception {
        mockMvc.perform(get("/probe/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value("riskLevel inválido"))
                .andExpect(jsonPath("$.path").value("/probe/illegal-argument"));
    }

    @Test
    void mapsMalformedBodyToBadRequest() throws Exception {
        mockMvc.perform(post("/probe/body").contentType(MediaType.APPLICATION_JSON).content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("MALFORMED_REQUEST"));
    }

    @Test
    void mapsMissingParameterToBadRequest() throws Exception {
        mockMvc.perform(get("/probe/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("MALFORMED_REQUEST"));
    }

    @Test
    void preservesStatusOfResponseStatusException() throws Exception {
        mockMvc.perform(get("/probe/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("recurso em conflito"));
    }

    @Test
    void mapsBusinessExceptionToItsOwnStatus() throws Exception {
        mockMvc.perform(get("/probe/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("INVALID_NFE"))
                .andExpect(jsonPath("$.message").value("XML inválido"));
    }

    @Test
    void mapsUnexpectedFailureToInternalErrorWithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error_code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Erro interno. Tente novamente em instantes."));
    }

    @RestController
    static class ProbeController {

        @GetMapping("/probe/illegal-argument")
        String illegalArgument() {
            throw new IllegalArgumentException("riskLevel inválido");
        }

        @PostMapping("/probe/body")
        String body(@RequestBody Payload payload) {
            return payload.value();
        }

        @GetMapping("/probe/param")
        String param(@RequestParam String required) {
            return required;
        }

        @GetMapping("/probe/conflict")
        String conflict() {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "recurso em conflito");
        }

        @GetMapping("/probe/business")
        String business() {
            throw new InvalidNFeException("XML inválido");
        }

        @GetMapping("/probe/boom")
        String boom() {
            throw new IllegalStateException("segredo interno");
        }

        record Payload(String value) {}
    }
}
