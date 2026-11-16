package com.riobranco.mousseomanos;

import android.os.Bundle;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Set;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** View: desenha o estado e encaminha ações. Não conhece HTTP nem Repository. */
public class MainActivity extends AppCompatActivity {
    private ProdutoViewModel vm;
    private ProdutoViewModel.Estado estado;
    private Long idEmEdicao;
    private String aba = "cardapio";
    private int ultimoSalvo;
    private String categoriaSelecionada = "Todos";
    private Typeface fonteDisplay;
    private static final int TEXTO = Color.rgb(244, 238, 231);
    private static final int SUAVE = Color.rgb(182, 167, 155);
    private static final int DESTAQUE = Color.rgb(225, 108, 96);
    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private EditText campo(int id) { return findViewById(id); }
    private String texto(int id) { return campo(id).getText().toString(); }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fonteDisplay = ResourcesCompat.getFont(this, R.font.squad_display);
        WindowInsetsControllerCompat barras = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        barras.setAppearanceLightStatusBars(false);
        barras.setAppearanceLightNavigationBars(false);
        View raiz = findViewById(R.id.main);
        int padding = raiz.getPaddingLeft();
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            var bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(padding + bars.left, padding + bars.top, padding + bars.right, padding + bars.bottom);
            return insets;
        });
        vm = new ViewModelProvider(this).get(ProdutoViewModel.class);
        if (savedInstanceState != null) {
            aba = savedInstanceState.getString("aba", "cardapio");
            if (savedInstanceState.containsKey("id")) idEmEdicao = savedInstanceState.getLong("id");
            ultimoSalvo = savedInstanceState.getInt("salvo");
            categoriaSelecionada = savedInstanceState.getString("categoria", "Todos");
        }
        findViewById(R.id.configurar).setOnClickListener(v -> configurar());
        findViewById(R.id.cardapio).setOnClickListener(v -> abrir("cardapio"));
        findViewById(R.id.gerenciar).setOnClickListener(v -> abrir("gerenciar"));
        findViewById(R.id.carrinho).setOnClickListener(v -> abrir("carrinho"));
        findViewById(R.id.resumoCarrinho).setOnClickListener(v -> abrir("carrinho"));
        findViewById(R.id.verCardapio).setOnClickListener(v ->
                findViewById(R.id.conteudo).post(() -> ((ScrollView) findViewById(R.id.conteudo))
                        .smoothScrollTo(0, findViewById(R.id.hero).getBottom())));
        findViewById(R.id.pedidos).setOnClickListener(v -> { abrir("pedidos"); vm.carregarPedidos(); });
        findViewById(R.id.atualizar).setOnClickListener(v -> {
            if (aba.equals("pedidos")) vm.carregarPedidos(); else vm.carregar();
        });
        findViewById(R.id.salvar).setOnClickListener(v -> vm.salvar(idEmEdicao, texto(R.id.nome), texto(R.id.categoria),
                texto(R.id.descricao), texto(R.id.valor), ((CheckBox) findViewById(R.id.disponivel)).isChecked()));
        findViewById(R.id.limpar).setOnClickListener(v -> limpar());
        findViewById(R.id.confirmar).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Confirmar pedido?").setMessage("Confira os itens e o total antes de enviar.")
                .setNegativeButton("Voltar", null).setPositiveButton("Enviar", (d, w) ->
                    vm.confirmarPedido(texto(R.id.cliente), texto(R.id.observacao))).show());
        campo(R.id.busca).addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { render(); }
            public void afterTextChanged(Editable e) { }
        });
        vm.getEstado().observe(this, novo -> {
            estado = novo;
            if (novo.formularioSalvo != ultimoSalvo) { ultimoSalvo = novo.formularioSalvo; limpar(); }
            render();
        });
        if (vm.getUrl().isEmpty()) {
            String url = getPreferences(MODE_PRIVATE).getString("api", getString(R.string.api_padrao));
            if (!url.isEmpty()) vm.configurar(url);
        }
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("aba", aba); out.putInt("salvo", ultimoSalvo);
        out.putString("categoria", categoriaSelecionada);
        if (idEmEdicao != null) out.putLong("id", idEmEdicao);
    }
    private void configurar() {
        EditText entrada = new EditText(this);
        entrada.setSingleLine(true); entrada.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        entrada.setHint("https://seu-app.azurewebsites.net/"); entrada.setText(vm.getUrl());
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("URL raiz da API")
                .setMessage("Use o domínio do Azure, sem /produtos. Emulador local: http://10.0.2.2:8080/")
                .setView(entrada).setNegativeButton("Cancelar", null).setPositiveButton("Conectar", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (vm.configurar(entrada.getText().toString())) {
                getPreferences(MODE_PRIVATE).edit().putString("api", vm.getUrl()).apply(); limpar(); dialog.dismiss();
            } else entrada.setError("Confira a URL e a mensagem na tela.");
        }));
        dialog.show();
    }
    private void abrir(String nome) {
        if (!aba.equals(nome)) ((ScrollView) findViewById(R.id.conteudo)).scrollTo(0, 0);
        aba = nome; render();
    }
    private void limpar() {
        idEmEdicao = null;
        for (int id : new int[]{R.id.nome, R.id.categoria, R.id.descricao, R.id.valor}) campo(id).setText("");
        ((CheckBox) findViewById(R.id.disponivel)).setChecked(true);
        ((TextView) findViewById(R.id.tituloFormulario)).setText("Novo produto");
        ((Button) findViewById(R.id.salvar)).setText("Cadastrar item");
    }
    private void editar(Produto p) {
        idEmEdicao = p.id;
        campo(R.id.nome).setText(p.nome); campo(R.id.categoria).setText(p.categoria);
        campo(R.id.descricao).setText(p.descricao); campo(R.id.valor).setText(p.valor == null ? "" : p.valor.toPlainString());
        ((CheckBox) findViewById(R.id.disponivel)).setChecked(p.disponivel);
        render(); campo(R.id.nome).requestFocus();
        findViewById(R.id.formulario).post(() -> ((ScrollView) findViewById(R.id.conteudo)).smoothScrollTo(0, 0));
    }
    private void habilitar(View view, boolean habilitado) {
        view.setEnabled(habilitado);
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) habilitar(((ViewGroup) view).getChildAt(i), habilitado);
    }
    private void visivel(int id, boolean sim) { findViewById(id).setVisibility(sim ? View.VISIBLE : View.GONE); }
    private LinearLayout bloco(LinearLayout pai, String titulo, String descricao) {
        LinearLayout bloco = new LinearLayout(this); bloco.setOrientation(LinearLayout.VERTICAL);
        bloco.setPadding(dp(18), dp(18), dp(18), dp(18));
        bloco.setBackgroundResource(R.drawable.panel);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(16); bloco.setLayoutParams(lp); pai.addView(bloco);
        TextView nome = new TextView(this); nome.setText(titulo); nome.setTextSize(26);
        nome.setTypeface(fonteDisplay); nome.setTextColor(TEXTO); bloco.addView(nome);
        TextView detalhe = new TextView(this); detalhe.setText(descricao); detalhe.setTextSize(13);
        detalhe.setTextColor(SUAVE); detalhe.setLineSpacing(dp(4), 1);
        detalhe.setPadding(0, dp(6), 0, dp(10)); bloco.addView(detalhe);
        return bloco;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void botao(LinearLayout pai, String texto, Runnable acao) {
        Button b = new androidx.appcompat.widget.AppCompatButton(this);
        b.setBackgroundTintList(null); b.setBackgroundResource(R.drawable.button_primary);
        b.setText(texto); b.setTextColor(Color.WHITE); b.setTypeface(fonteDisplay); b.setTextSize(18);
        b.setMinHeight(dp(48)); b.setMinimumHeight(dp(48)); b.setAllCaps(true);
        b.setStateListAnimator(null); b.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(8); b.setLayoutParams(lp);
        if (texto.startsWith("Excluir") || texto.startsWith("−")) {
            b.setBackgroundResource(R.drawable.button_secondary); b.setTextColor(SUAVE);
        }
        b.setEnabled(!estado.ocupado); b.setAlpha(estado.ocupado ? .5f : 1f);
        b.setOnClickListener(v -> acao.run()); pai.addView(b);
    }
    private LinearLayout produtoCard(LinearLayout lista, Produto p, boolean gerenciar) {
        LinearLayout card = bloco(lista, p.nome, p.descricao == null ? "" : p.descricao);
        TextView categoria = new TextView(this);
        categoria.setText((p.categoria == null ? "CARDÁPIO" : p.categoria.toUpperCase(new Locale("pt", "BR")))
                + (p.disponivel ? "" : "  ·  INDISPONÍVEL"));
        categoria.setTextSize(10); categoria.setTextColor(DESTAQUE); categoria.setLetterSpacing(.08f);
        categoria.setPadding(0, 0, 0, dp(6)); card.addView(categoria, 0);
        String tipo = (p.categoria == null ? "" : p.categoria).toLowerCase(new Locale("pt", "BR"));
        String nome = (p.nome == null ? "" : p.nome).toLowerCase(new Locale("pt", "BR"));
        if (!gerenciar && (tipo.contains("hamb") || tipo.contains("burger") || tipo.contains("combo"))) {
            ImageView foto = new ImageView(this); foto.setScaleType(ImageView.ScaleType.CENTER_CROP);
            foto.setImageResource(nome.contains("bacon") ? R.drawable.burger_bacon
                    : nome.contains("duplo") ? R.drawable.hero_burger : R.drawable.burger_classic);
            foto.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams fotoLp = new LinearLayout.LayoutParams(-1, dp(156));
            fotoLp.bottomMargin = dp(14); foto.setLayoutParams(fotoLp); card.addView(foto, 0);
        }
        TextView preco = new TextView(this); preco.setText(dinheiro(p.valor)); preco.setTextSize(30);
        preco.setTextColor(TEXTO); preco.setTypeface(fonteDisplay); card.addView(preco);
        return card;
    }
    private void categorias() {
        LinearLayout grupo = findViewById(R.id.categorias); grupo.removeAllViews();
        Set<String> nomes = new LinkedHashSet<>(); nomes.add("Todos");
        for (Produto p : estado.produtos) if (p.disponivel && p.categoria != null) nomes.add(p.categoria);
        if (!nomes.contains(categoriaSelecionada)) categoriaSelecionada = "Todos";
        for (String categoria : nomes) {
            Button b = new androidx.appcompat.widget.AppCompatButton(this);
            b.setBackgroundTintList(null);
            b.setBackgroundResource(categoria.equals(categoriaSelecionada) ? R.drawable.button_primary : R.drawable.button_secondary);
            b.setText(categoria); b.setTextColor(TEXTO); b.setTextSize(12); b.setAllCaps(false);
            b.setPadding(dp(14), dp(8), dp(14), dp(8)); b.setMinWidth(0); b.setMinimumWidth(0);
            b.setMinHeight(dp(48)); b.setMinimumHeight(dp(48)); b.setStateListAnimator(null);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(48)); lp.setMarginEnd(dp(8)); b.setLayoutParams(lp);
            b.setOnClickListener(v -> { categoriaSelecionada = categoria; render(); }); grupo.addView(b);
        }
    }
    private String data(String iso) {
        try { return DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", new Locale("pt", "BR"))
                .withZone(ZoneId.systemDefault()).format(Instant.parse(iso)); }
        catch (Exception e) { return iso == null ? "" : iso; }
    }
    private String dinheiro(BigDecimal valor) { return moeda.format(valor == null ? BigDecimal.ZERO : valor); }
    private void render() {
        if (estado == null) return;
        ((TextView) findViewById(R.id.status)).setText(estado.mensagem);
        visivel(R.id.progresso, estado.ocupado);
        boolean gerenciar = aba.equals("gerenciar"), carrinho = aba.equals("carrinho"), pedidos = aba.equals("pedidos");
        boolean menu = !gerenciar && !carrinho && !pedidos;
        visivel(R.id.hero, menu); visivel(R.id.filtros, menu); visivel(R.id.notaFotos, menu);
        ((TextView) findViewById(R.id.tituloPagina)).setText(gerenciar ? "Gerenciar cardápio" : carrinho ? "Seu pedido" : pedidos ? "Pedidos confirmados" : "Nosso cardápio");
        ((TextView) findViewById(R.id.sobretitulo)).setText(gerenciar ? "SEU CARDÁPIO, DO SEU JEITO" : carrinho ? "BATEU A FOME?" : pedidos ? "JÁ SAIU DA ESCOLHA" : "ESCOLHA SEU PRÓXIMO FAVORITO");
        ((TextView) findViewById(R.id.subtituloPagina)).setText(gerenciar ? "Cadastre itens e controle o que está disponível."
                : carrinho ? "Seu squad de favoritos está aqui." : pedidos ? "Os itens e os valores de cada pedido." : "Na chapa, no capricho. Escolha o seu.");
        campo(R.id.busca).setHint(gerenciar ? "Buscar itens cadastrados" : "Qual é a sua fome hoje?");
        for (int id : new int[]{R.id.cardapio, R.id.carrinho, R.id.pedidos, R.id.gerenciar}) {
            Button b = findViewById(id);
            boolean ativo = (id == R.id.cardapio && menu) || (id == R.id.gerenciar && gerenciar)
                    || (id == R.id.carrinho && carrinho) || (id == R.id.pedidos && pedidos);
            b.setSelected(ativo); b.setTextColor(ativo ? DESTAQUE : SUAVE);
            b.setCompoundDrawableTintList(ColorStateList.valueOf(ativo ? DESTAQUE : SUAVE));
        }
        BigDecimal somaCarrinho = BigDecimal.ZERO; int quantidadeCarrinho = 0;
        for (Pedido.Item item : estado.carrinho) {
            quantidadeCarrinho += item.quantidade;
            somaCarrinho = somaCarrinho.add(item.precoUnitario.multiply(BigDecimal.valueOf(item.quantidade)));
        }
        visivel(R.id.resumoCarrinho, quantidadeCarrinho > 0 && menu);
        ((Button) findViewById(R.id.resumoCarrinho)).setText("VER PEDIDO  ·  " + quantidadeCarrinho
                + (quantidadeCarrinho == 1 ? " ITEM    " : " ITENS    ") + dinheiro(somaCarrinho) + "  →");
        if (menu) categorias();
        visivel(R.id.formulario, gerenciar); visivel(R.id.checkout, carrinho); visivel(R.id.busca, !carrinho && !pedidos);
        habilitar(findViewById(R.id.formulario), !estado.ocupado);
        for (int id : new int[]{R.id.configurar, R.id.atualizar, R.id.confirmar}) findViewById(id).setEnabled(!estado.ocupado);
        ((TextView) findViewById(R.id.tituloFormulario)).setText(idEmEdicao == null ? "Novo produto" : "Editar produto #" + idEmEdicao);
        ((Button) findViewById(R.id.salvar)).setText(idEmEdicao == null ? "Cadastrar item" : "Salvar alterações");
        LinearLayout lista = findViewById(R.id.itens); lista.removeAllViews();
        if (pedidos) {
            for (Pedido p : estado.pedidos) {
                StringBuilder detalhe = new StringBuilder(p.cliente + " · " + data(p.criadoEm) + "\n" + p.status + " · " + dinheiro(p.total));
                if (p.itens != null) for (Pedido.Item item : p.itens) detalhe.append("\n").append(item.quantidade).append(" × ").append(item.nome).append(" · ").append(dinheiro(item.precoUnitario));
                if (p.observacao != null && !p.observacao.isEmpty()) detalhe.append("\n").append(p.observacao);
                bloco(lista, "Pedido #" + p.id, detalhe.toString());
            }
        } else if (carrinho) {
            BigDecimal total = BigDecimal.ZERO;
            for (Pedido.Item item : estado.carrinho) {
                BigDecimal subtotal = item.precoUnitario.multiply(BigDecimal.valueOf(item.quantidade)); total = total.add(subtotal);
                LinearLayout card = bloco(lista, item.nome, item.quantidade + " × " + dinheiro(item.precoUnitario) + " = " + dinheiro(subtotal));
                Produto produto = null; for (Produto p : estado.produtos) if (item.produtoId.equals(p.id)) produto = p;
                if (produto != null) {
                    Produto p = produto; botao(card, "− Remover uma unidade", () -> vm.quantidade(p, -1));
                    botao(card, "+ Adicionar uma unidade", () -> vm.quantidade(p, 1));
                }
            }
            ((TextView) findViewById(R.id.total)).setText("Total: " + dinheiro(total));
            findViewById(R.id.confirmar).setEnabled(!estado.ocupado && !estado.carrinho.isEmpty());
        } else {
            String busca = texto(R.id.busca).trim().toLowerCase(new Locale("pt", "BR"));
            for (Produto p : estado.produtos) {
                if (!gerenciar && !p.disponivel) continue;
                if (menu && !categoriaSelecionada.equals("Todos") && !categoriaSelecionada.equals(p.categoria)) continue;
                if (!(p.nome + " " + p.categoria + " " + p.descricao).toLowerCase(new Locale("pt", "BR")).contains(busca)) continue;
                LinearLayout card = produtoCard(lista, p, gerenciar);
                if (gerenciar) {
                    botao(card, "Editar item", () -> editar(p));
                    botao(card, "Excluir item", () -> new AlertDialog.Builder(this).setTitle("Excluir produto?")
                            .setMessage(p.nome).setNegativeButton("Cancelar", null)
                            .setPositiveButton("Excluir", (d, w) -> { if (p.id.equals(idEmEdicao)) limpar(); vm.excluir(p.id); }).show());
                } else botao(card, "Adicionar ao pedido  +", () -> vm.quantidade(p, 1));
            }
        }
        if (lista.getChildCount() == 0) bloco(lista, carrinho ? "Seu pedido começa aqui" : pedidos ? "Seu próximo pedido começa aqui" : "Nada por aqui ainda",
                carrinho ? "Escolha seus favoritos no cardápio para montar seu pedido." : pedidos ? "Os pedidos confirmados aparecerão aqui." : "Atualize a lista, escolha outra categoria ou faça uma nova busca.");
    }
}
