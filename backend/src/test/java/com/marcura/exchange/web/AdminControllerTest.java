package com.marcura.exchange.web;

import com.marcura.exchange.service.ingestion.RateIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired MockMvc mvc;
    @MockBean RateIngestionService ingestionService;

    @Test
    void refresh_returnsRowsWritten() throws Exception {
        when(ingestionService.ingestLatest()).thenReturn(18);

        mvc.perform(post("/admin/refresh"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.rowsWritten").value(18));
    }

    @Test
    void refresh_returnsZeroWhenNothingIngested() throws Exception {
        when(ingestionService.ingestLatest()).thenReturn(0);

        mvc.perform(post("/admin/refresh"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.rowsWritten").value(0));
    }
}
