package com.mindlab.livrosac2;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String COLECAO = "livros";

    private final String[] generos = {"Selecione", "Romance", "Fantasia", "Terror", "Ficcao Cientifica", "Biografia"};
    private final String[] statusList = {"Selecione", "Quero ler", "Lendo", "Concluido"};
    private final String[] filtroGeneros = {"Todos", "Romance", "Fantasia", "Terror", "Ficcao Cientifica", "Biografia"};

    private FirebaseFirestore db;

    private EditText etTitulo, etAutor, etAno;
    private Spinner spGenero, spStatus, spFiltroGenero;
    private CheckBox cbFavorito, cbSoFavoritos;
    private Button btnSalvar, btnLimpar;
    private TextView tvTotal;

    private final List<Livro> todos = new ArrayList<>();
    private final List<Livro> exibidos = new ArrayList<>();
    private LivroAdapter adapter;

    private Livro livroEditando = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        etTitulo = findViewById(R.id.etTitulo);
        etAutor = findViewById(R.id.etAutor);
        etAno = findViewById(R.id.etAno);
        spGenero = findViewById(R.id.spGenero);
        spStatus = findViewById(R.id.spStatus);
        spFiltroGenero = findViewById(R.id.spFiltroGenero);
        cbFavorito = findViewById(R.id.cbFavorito);
        cbSoFavoritos = findViewById(R.id.cbSoFavoritos);
        btnSalvar = findViewById(R.id.btnSalvar);
        btnLimpar = findViewById(R.id.btnLimpar);
        tvTotal = findViewById(R.id.tvTotal);

        spGenero.setAdapter(spinnerAdapter(generos));
        spStatus.setAdapter(spinnerAdapter(statusList));
        spFiltroGenero.setAdapter(spinnerAdapter(filtroGeneros));

        RecyclerView rv = findViewById(R.id.rvLivros);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LivroAdapter(exibidos, this::carregarParaEdicao, this::confirmarExclusao);
        rv.setAdapter(adapter);

        btnSalvar.setOnClickListener(v -> salvar());
        btnLimpar.setOnClickListener(v -> limparCampos());
        cbSoFavoritos.setOnCheckedChangeListener((b, c) -> aplicarFiltro());
        spFiltroGenero.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                aplicarFiltro();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        carregarLivros();
    }

    private ArrayAdapter<String> spinnerAdapter(String[] itens) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, itens);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    // READ: busca todos os documentos da colecao "livros"
    private void carregarLivros() {
        db.collection(COLECAO).get()
                .addOnSuccessListener(query -> {
                    todos.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        Livro l = doc.toObject(Livro.class);
                        l.setId(doc.getId());
                        todos.add(l);
                    }
                    aplicarFiltro();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao carregar: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // CREATE / UPDATE
    private void salvar() {
        String titulo = etTitulo.getText().toString().trim();
        String autor = etAutor.getText().toString().trim();
        String anoStr = etAno.getText().toString().trim();

        if (titulo.isEmpty()) {
            etTitulo.setError("Informe o titulo");
            etTitulo.requestFocus();
            return;
        }
        if (autor.isEmpty()) {
            etAutor.setError("Informe o autor");
            etAutor.requestFocus();
            return;
        }
        if (spGenero.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Selecione o genero", Toast.LENGTH_SHORT).show();
            return;
        }
        if (anoStr.isEmpty()) {
            etAno.setError("Informe o ano");
            etAno.requestFocus();
            return;
        }
        if (spStatus.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Selecione o status de leitura", Toast.LENGTH_SHORT).show();
            return;
        }

        int ano = Integer.parseInt(anoStr);
        String genero = spGenero.getSelectedItem().toString();
        String status = spStatus.getSelectedItem().toString();
        boolean favorito = cbFavorito.isChecked();

        if (livroEditando == null) {
            Livro novo = new Livro(null, titulo, autor, genero, ano, status, favorito);
            db.collection(COLECAO).add(novo)
                    .addOnSuccessListener(doc -> {
                        Toast.makeText(this, "Livro salvo!", Toast.LENGTH_SHORT).show();
                        limparCampos();
                        carregarLivros();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            livroEditando.setTitulo(titulo);
            livroEditando.setAutor(autor);
            livroEditando.setGenero(genero);
            livroEditando.setAno(ano);
            livroEditando.setStatus(status);
            livroEditando.setFavorito(favorito);
            db.collection(COLECAO).document(livroEditando.getId()).set(livroEditando)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Livro atualizado!", Toast.LENGTH_SHORT).show();
                        limparCampos();
                        carregarLivros();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Erro ao atualizar: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    // Clique curto: carrega nos campos
    private void carregarParaEdicao(Livro livro) {
        livroEditando = livro;
        etTitulo.setText(livro.getTitulo());
        etAutor.setText(livro.getAutor());
        etAno.setText(String.valueOf(livro.getAno()));
        cbFavorito.setChecked(livro.isFavorito());
        spGenero.setSelection(indexOf(generos, livro.getGenero()));
        spStatus.setSelection(indexOf(statusList, livro.getStatus()));
        btnSalvar.setText("Atualizar");
    }

    // Clique longo: confirma e exclui
    private void confirmarExclusao(Livro livro) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir")
                .setMessage("Excluir \"" + livro.getTitulo() + "\"?")
                .setPositiveButton("Sim", (d, w) -> excluir(livro))
                .setNegativeButton("Nao", null)
                .show();
    }

    // DELETE
    private void excluir(Livro livro) {
        db.collection(COLECAO).document(livro.getId()).delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Livro excluido!", Toast.LENGTH_SHORT).show();
                    if (livroEditando != null && livroEditando.getId().equals(livro.getId())) limparCampos();
                    carregarLivros();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao excluir: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void limparCampos() {
        livroEditando = null;
        etTitulo.setText("");
        etAutor.setText("");
        etAno.setText("");
        cbFavorito.setChecked(false);
        spGenero.setSelection(0);
        spStatus.setSelection(0);
        btnSalvar.setText("Salvar");
    }

    // Filtro por genero + extra: somente favoritos + extra: total
    private void aplicarFiltro() {
        String genero = spFiltroGenero.getSelectedItem().toString();
        boolean soFavoritos = cbSoFavoritos.isChecked();
        exibidos.clear();
        for (Livro l : todos) {
            boolean okGenero = genero.equals("Todos") || genero.equals(l.getGenero());
            boolean okFav = !soFavoritos || l.isFavorito();
            if (okGenero && okFav) exibidos.add(l);
        }
        adapter.notifyDataSetChanged();
        tvTotal.setText("Total: " + todos.size() + " livros");
    }

    private int indexOf(String[] arr, String valor) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(valor)) return i;
        return 0;
    }
}
