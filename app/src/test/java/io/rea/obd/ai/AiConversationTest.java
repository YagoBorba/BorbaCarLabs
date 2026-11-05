package io.rea.obd.ai;

import static org.junit.Assert.*;
import android.content.Context;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AiConversationTest {
    private Context context() { return RuntimeEnvironment.getApplication(); }
    private static JSONObject answer(String ref) throws Exception {
        return new JSONObject().put("model", "test-provider").put("analysis", new JSONObject()
                .put("summary", "Dados insuficientes para confirmar uma peça.")
                .put("hypotheses", new JSONArray().put(new JSONObject().put("title", "Verificar contexto")
                        .put("reasoning", "O código precisa ser investigado.").put("evidence", new JSONArray().put(ref))
                        .put("confidence", "baixa").put("next_check", "Consultar uma oficina.")))
                .put("missing_information", new JSONArray()).put("safety_note", ""));
    }

    @Test public void savesRepliesOnlyForTheSameCaseRevision() throws Exception {
        AiConversation first = new AiConversation(context(), "case-a", 1, Collections.singleton("D1"));
        first.append("O que significa?", answer("D1"));
        assertEquals(1, new AiConversation(context(), "case-a", 1, Collections.singleton("D1")).turns().length());
        assertEquals(0, new AiConversation(context(), "case-a", 2, Collections.singleton("D1")).turns().length());
        assertEquals(0, new AiConversation(context(), "case-b", 1, Collections.singleton("D1")).turns().length());
        first.clear();
        assertEquals(0, first.turns().length());
    }

    @Test public void deletingACaseRemovesOnlyItsConversations() throws Exception {
        AiConversation first = new AiConversation(context(), "case-a", 1, Collections.singleton("D1"));
        AiConversation other = new AiConversation(context(), "case-b", 1, Collections.singleton("D1"));
        first.append("Uma pergunta", answer("D1")); other.append("Outra pergunta", answer("D1"));
        AiConversation.deleteCase(context(), "case-a");
        assertEquals(0, first.turns().length()); assertEquals(1, other.turns().length());
    }

    @Test public void boundsHistoryAndNeverShowsInventedEvidence() throws Exception {
        AiConversation conversation = new AiConversation(context(), "bounded", 1, Collections.singleton("D1"));
        for (int i = 0; i < 12; i++) conversation.append("Pergunta " + i, answer("D1"));
        assertEquals(8, conversation.turns().length());
        conversation.append("Pergunta final", answer("X9"));
        JSONArray turns = conversation.turns();
        assertEquals(8, turns.length());
        AnalysisResult last = AnalysisResult.parse(turns.getJSONObject(7).getJSONObject("response"), Collections.singleton("D1"));
        assertTrue(last.hypotheses.get(0).evidence.isEmpty());
        try { conversation.append("Sem resposta", new JSONObject().put("model", "m").put("analysis", new JSONObject().put("summary", " "))); fail(); }
        catch (AnalysisResult.ContractException expected) { }
        assertEquals(8, conversation.turns().length());
    }

    @Test public void filtersPersonalDataInSavedQuestionsAndSharedPrompts() throws Exception {
        AiConversation conversation = new AiConversation(context(), "private", 1, Collections.singleton("D1"));
        conversation.append("Contato pessoa@example.org placa ABC1D23", answer("D1"));
        String saved = conversation.turns().toString();
        assertFalse(saved.contains("pessoa@example.org"));
        assertFalse(saved.contains("ABC1D23"));
        String prompt = AiConversation.externalPrompt(new JSONObject().put("language", "pt-BR").put("demonstration_data", true));
        assertTrue(prompt.contains("não invente leituras"));
        assertTrue(prompt.contains("demonstration_data"));
    }

    @Test public void keepsOneEncryptedKeyPerProviderAndRejectsBadInput() throws Exception {
        KeyVault.backend = TestVault.INSTANCE;
        AiSettings.save(context(), AiProvider.CLAUDE, "sk-ant-api03-0123456789abcdef", "", "");
        AiSettings.save(context(), AiProvider.GEMINI, "AIza0123456789abcdefghij", "gemini-3.5-flash-lite", "");
        AiSettings gemini = AiSettings.load(context());
        assertEquals(AiProvider.GEMINI, gemini.provider);
        assertTrue(gemini.configured());
        assertEquals("AIza0123456789abcdefghij", gemini.key());
        assertEquals("ghij", gemini.keyHint());
        assertEquals("sk-ant-api03-0123456789abcdef", AiSettings.load(context(), AiProvider.CLAUDE).key());
        String stored = context().getSharedPreferences("ai", Context.MODE_PRIVATE).getAll().toString();
        assertFalse("a chave não pode ficar em texto puro", stored.contains("AIza0123456789abcdefghij"));
        AiSettings.save(context(), AiProvider.GEMINI, "", "", "");
        assertEquals("AIza0123456789abcdefghij", AiSettings.load(context()).key());
        assertEquals(AiProvider.GEMINI.defaultModel, AiSettings.load(context()).modelOrDefault());
        for (String[] bad : new String[][]{{"curta", ""}, {"AIza0123456789ábcdefghij", ""}, {"AIza0123456789abcdefghij", "../../etc"}}) {
            try { AiSettings.save(context(), AiProvider.GEMINI, bad[0], bad[1], ""); fail(); } catch (IllegalArgumentException expected) { }
        }
        AiSettings.save(context(), AiProvider.GEMINI, " AIza0123456789\nabcdefghij ", "", "");
        assertEquals("AIza0123456789abcdefghij", AiSettings.load(context()).key());
        assertTrue(AiSettings.removeKey(context(), AiProvider.GEMINI));
        assertFalse(AiSettings.load(context()).configured());
        try { AiSettings.save(context(), AiProvider.GEMINI, "", "", ""); fail(); } catch (IllegalArgumentException expected) { }
        try { AiSettings.load(context()).key(); fail(); } catch (AnalysisClient.Failure expected) { }
    }
}
