package com.ahs.cvm.api.admin;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ahs.cvm.integration.osv.OsvJsonlMirrorLookup;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OsvMirrorAdminControllerWebTest {

    private MockMvc mvcFor(OsvMirrorAdminController controller) {
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(new ObjectMapper());
        return MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    @DisplayName("POST /admin/osv-mirror/reload: aktiver Mirror -> 200 + indexSize")
    void reloadAktiv() throws Exception {
        OsvJsonlMirrorLookup lookup = org.mockito.Mockito.mock(OsvJsonlMirrorLookup.class);
        given(lookup.indexSize()).willReturn(42);
        MockMvc mvc = mvcFor(new OsvMirrorAdminController(Optional.of(lookup)));

        mvc.perform(post("/api/v1/admin/osv-mirror/reload")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reloaded").value(true))
                .andExpect(jsonPath("$.indexSize").value(42));

        verify(lookup, times(1)).reload();
    }

    @Test
    @DisplayName("POST /admin/osv-mirror/reload: inaktiver Mirror -> 503 + osv_mirror_inactive")
    void reloadInaktiv() throws Exception {
        OsvJsonlMirrorLookup lookup = org.mockito.Mockito.mock(OsvJsonlMirrorLookup.class);
        MockMvc mvc = mvcFor(new OsvMirrorAdminController(Optional.empty()));

        mvc.perform(post("/api/v1/admin/osv-mirror/reload")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("osv_mirror_inactive"));

        verify(lookup, never()).reload();
    }
}
