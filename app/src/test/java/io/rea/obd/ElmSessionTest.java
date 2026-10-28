package io.rea.obd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import org.junit.Test;

public class ElmSessionTest {
    @Test
    public void connectsToCanCarAndReadsSupportMap() throws IOException {
        FakeAdapter adapter = FakeAdapter.canCar();
        ElmSession session = new ElmSession(adapter);
        session.connect();

        assertTrue(session.isConnected());
        assertTrue(session.isCan());
        assertEquals("ELM327 v1.5", session.identity());
        assertEquals("AUTO, ISO 15765-4 (CAN 11/500)", session.protocol());
        assertTrue(session.supported().contains(0x0C));
        assertTrue(session.supported().contains(0x0D));
        assertTrue(session.supported().contains(0x05));
        assertFalse(session.supported().contains(0x02));
        assertEquals("ATZ", adapter.sent.get(0));
        assertEquals("ATE0", adapter.sent.get(1));
    }

    @Test
    public void refusesWhenEcuDoesNotAnswerSupportMap() {
        FakeAdapter adapter = FakeAdapter.canCar().reply("0100", "NO DATA");
        ElmSession session = new ElmSession(adapter);
        try {
            session.connect();
            fail("connect should fail without an ECU answer");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("ECU"));
        }
        assertFalse(session.isConnected());
        assertTrue(adapter.closed);
    }

    @Test
    public void adapterErrorClosesSession() throws IOException {
        ElmSession session = new ElmSession(FakeAdapter.canCar());
        session.connect();
        try {
            session.command("09FF", 1000L);
            fail("unknown command answered with ? must fail");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().startsWith("Erro do adaptador"));
        }
        assertFalse(session.isConnected());
    }

    @Test
    public void silentAdapterTimesOutAndCloses() throws IOException {
        FakeAdapter adapter = FakeAdapter.canCar();
        ElmSession session = new ElmSession(adapter);
        session.connect();
        adapter.reply("0105", FakeAdapter.DISCONNECT);
        try {
            session.command("0105", 1000L);
            fail("ended stream must fail");
        } catch (IOException expected) {
            assertFalse(session.isConnected());
        }
    }

    @Test
    public void rejectsCommandsThatCouldInjectLines() throws IOException {
        ElmSession session = new ElmSession(FakeAdapter.canCar());
        session.connect();
        try {
            session.command("0100\r04", 1000L);
            fail("multi-line command must be rejected");
        } catch (IOException expected) {
            assertEquals("Comando inválido", expected.getMessage());
        }
    }
}
