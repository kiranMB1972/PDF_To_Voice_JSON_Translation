package com.org.pdfplatform.translate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebTranslateClientTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void googleAnswerSegmentsAreJoinedInOrder() throws Exception {
        var root = om.readTree("[[[\"Hello \",\"Namaste \",null,null,10],[\"world\",\"jagattu\",null,null,10]],null,\"kn\"]");
        assertEquals("Hello world", WebTranslateClient.parseGoogle(root));
    }

    @Test
    void googleAnswerThatIsEmptyOrOddIsRejected() throws Exception {
        assertThrows(IllegalStateException.class, () -> WebTranslateClient.parseGoogle(om.readTree("[]")));
        assertThrows(IllegalStateException.class, () -> WebTranslateClient.parseGoogle(om.readTree("{\"x\":1}")));
        assertThrows(IllegalStateException.class, () -> WebTranslateClient.parseGoogle(om.readTree("[[[\"\",\"a\"]]]")));
    }

    @Test
    void myMemoryAnswerIsReadAndQuotaWarningsAreRejected() throws Exception {
        assertEquals("Hola", WebTranslateClient.parseMyMemory(om.readTree("{\"responseData\":{\"translatedText\":\"Hola\"},\"responseStatus\":200}")));
        assertThrows(IllegalStateException.class, () -> WebTranslateClient.parseMyMemory(om.readTree(
                "{\"responseData\":{\"translatedText\":\"MYMEMORY WARNING: YOU USED ALL AVAILABLE FREE TRANSLATIONS FOR TODAY\"},\"responseStatus\":200}")));
        assertThrows(IllegalStateException.class, () -> WebTranslateClient.parseMyMemory(om.readTree(
                "{\"responseData\":{\"translatedText\":\"x\"},\"responseStatus\":429}")));
    }
}
