package com.riobranco.mousseomanos;

import android.content.Context;
import android.widget.EditText;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.mockwebserver.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TelaCrudTest {
    private MockWebServer server;
    private ActivityScenario<MainActivity> tela;
    private final AtomicInteger listagens = new AtomicInteger();
    private final AtomicInteger gravacoes = new AtomicInteger();
    private volatile boolean falhar;
    private volatile boolean cadastrado;
    private volatile boolean visual;
    @Before public void abrir() throws Exception {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override public MockResponse dispatch(RecordedRequest req) {
                if ("POST".equals(req.getMethod())) {
                    gravacoes.incrementAndGet();
                    if (falhar) return new MockResponse().setResponseCode(400).setBody("{\"message\":\"Falha de teste\"}");
                    cadastrado = true; return new MockResponse().setResponseCode(201).setBody("{}");
                }
                listagens.incrementAndGet();
                if (visual) return new MockResponse().setBody("["
                        + "{\"id\":1,\"nome\":\"X-Bacon\",\"categoria\":\"Hambúrgueres\",\"descricao\":\"Pão brioche, carne na chapa, cheddar e bacon crocante.\",\"valor\":29.90,\"disponivel\":true},"
                        + "{\"id\":2,\"nome\":\"Clássico da Squad\",\"categoria\":\"Hambúrgueres\",\"descricao\":\"O clássico com queijo, salada fresca e molho da casa.\",\"valor\":24.90,\"disponivel\":true},"
                        + "{\"id\":3,\"nome\":\"Refrigerante\",\"categoria\":\"Bebidas\",\"descricao\":\"Lata 350 ml, bem gelada.\",\"valor\":6.00,\"disponivel\":true}]");
                return new MockResponse().setBody(cadastrado
                        ? "[{\"id\":1,\"nome\":\"Mouse teste\",\"categoria\":\"Periférico\",\"descricao\":\"Sem fio\",\"valor\":19.90,\"disponivel\":true}]" : "[]");
            }
        });
        server.start();
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).edit().clear()
                .putString("api", server.url("/").toString()).commit();
        tela = ActivityScenario.launch(MainActivity.class);
        esperarMensagem("0 produto(s)");
    }
    @After public void fechar() throws Exception { if (tela != null) tela.close(); server.shutdown(); }
    private void esperarMensagem(String trecho) throws Exception {
        long fim = System.currentTimeMillis() + 10000;
        AtomicBoolean ok = new AtomicBoolean();
        while (System.currentTimeMillis() < fim) {
            tela.onActivity(a -> ok.set(((TextView) a.findViewById(R.id.status)).getText().toString().contains(trecho)));
            if (ok.get()) return;
            Thread.sleep(50);
        }
        fail("A tela não apresentou: " + trecho);
    }
    private void preencher() {
        tela.onActivity(a -> {
            a.findViewById(R.id.gerenciar).performClick();
            ((EditText) a.findViewById(R.id.nome)).setText("Mouse teste");
            ((EditText) a.findViewById(R.id.categoria)).setText("Periférico");
            ((EditText) a.findViewById(R.id.descricao)).setText("Sem fio");
            ((EditText) a.findViewById(R.id.valor)).setText("19.90");
        });
    }
    @Test public void cadastroAtualizaListaERecriacaoPreservaEstadoSemNovoGet() throws Exception {
        preencher(); tela.onActivity(a -> a.findViewById(R.id.salvar).performClick());
        esperarMensagem("Cadastrado! 1 produto(s)");
        assertEquals(1, gravacoes.get()); assertEquals(2, listagens.get());
        tela.onActivity(a -> assertEquals("", ((EditText) a.findViewById(R.id.nome)).getText().toString()));
        tela.recreate(); esperarMensagem("Cadastrado! 1 produto(s)");
        assertEquals(2, listagens.get());
        tela.onActivity(a -> a.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
        AtomicBoolean paisagem = new AtomicBoolean();
        long limite = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < limite) {
            tela.onActivity(a -> paisagem.set(a.getResources().getConfiguration().orientation
                    == android.content.res.Configuration.ORIENTATION_LANDSCAPE));
            if (paisagem.get()) break;
            Thread.sleep(50);
        }
        assertTrue("A Activity deve girar para paisagem", paisagem.get());
        esperarMensagem("Cadastrado! 1 produto(s)");
        assertEquals("Girar não deve repetir o GET", 2, listagens.get());
    }
    @Test public void erroPreservaCamposEValidacaoNaoChamaApi() throws Exception {
        preencher();
        tela.onActivity(a -> { ((EditText) a.findViewById(R.id.nome)).setText(""); a.findViewById(R.id.salvar).performClick(); });
        esperarMensagem("Preencha nome"); assertEquals(0, gravacoes.get());
        falhar = true;
        tela.onActivity(a -> { ((EditText) a.findViewById(R.id.nome)).setText("Mouse teste"); a.findViewById(R.id.salvar).performClick(); });
        esperarMensagem("Falha de teste");
        tela.recreate(); esperarMensagem("Falha de teste");
        tela.onActivity(a -> assertEquals("Mouse teste", ((EditText) a.findViewById(R.id.nome)).getText().toString()));
        assertEquals(1, gravacoes.get()); assertEquals(1, listagens.get());
    }
    private void foto(String nome) throws Exception {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        java.util.concurrent.CountDownLatch desenhado = new java.util.concurrent.CountDownLatch(1);
        tela.onActivity(a -> a.getWindow().getDecorView().postOnAnimation(() ->
                a.getWindow().getDecorView().postOnAnimation(desenhado::countDown)));
        assertTrue("A tela deve terminar o desenho antes da captura", desenhado.await(5, java.util.concurrent.TimeUnit.SECONDS));
        java.io.File pasta = ApplicationProvider.<Context>getApplicationContext().getExternalFilesDir(null);
        android.graphics.Bitmap imagem = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().takeScreenshot();
        assertNotNull(imagem);
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(pasta, nome + ".png"))) {
            imagem.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
        }
        imagem.recycle();
    }
    @Test public void categoriasECarrinhoPreservamNavegacaoDoNovoVisual() throws Exception {
        visual = true;
        tela.onActivity(a -> a.findViewById(R.id.atualizar).performClick());
        esperarMensagem("3 produto(s)");
        foto("visual-inicio");
        tela.onActivity(a -> ((android.widget.ScrollView) a.findViewById(R.id.conteudo))
                .scrollTo(0, a.findViewById(R.id.hero).getHeight()));
        foto("visual-cardapio");
        tela.onActivity(a -> {
            android.widget.LinearLayout chips = a.findViewById(R.id.categorias);
            boolean encontrado = false;
            for (int i = 0; i < chips.getChildCount(); i++) {
                android.widget.Button b = (android.widget.Button) chips.getChildAt(i);
                if (b.getText().toString().equals("Bebidas")) { b.performClick(); encontrado = true; break; }
            }
            assertTrue("Filtro de bebidas deve existir", encontrado);
            android.widget.LinearLayout lista = a.findViewById(R.id.itens);
            assertEquals(1, lista.getChildCount());
            android.widget.LinearLayout card = (android.widget.LinearLayout) lista.getChildAt(0);
            card.getChildAt(card.getChildCount() - 1).performClick();
            assertEquals(android.view.View.VISIBLE, a.findViewById(R.id.resumoCarrinho).getVisibility());
            a.findViewById(R.id.resumoCarrinho).performClick();
            assertEquals(android.view.View.VISIBLE, a.findViewById(R.id.checkout).getVisibility());
            assertTrue(((TextView) a.findViewById(R.id.total)).getText().toString().contains("6,00"));
        });
        foto("visual-carrinho");
        tela.recreate();
        tela.onActivity(a -> {
            assertEquals(android.view.View.VISIBLE, a.findViewById(R.id.checkout).getVisibility());
            a.findViewById(R.id.cardapio).performClick();
            assertEquals(1, ((android.widget.LinearLayout) a.findViewById(R.id.itens)).getChildCount());
            a.findViewById(R.id.gerenciar).performClick();
        });
        assertEquals("A navegação e a recriação não devem repetir o GET", 2, listagens.get());
        foto("visual-gerenciar");
        assertEquals("Preview visual não grava na API", 0, gravacoes.get());
    }
}
