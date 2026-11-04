package com.mindlab.livrosac2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LivroAdapter extends RecyclerView.Adapter<LivroAdapter.ViewHolder> {

    public interface OnClick { void onClick(Livro livro); }
    public interface OnLongClick { void onLongClick(Livro livro); }

    private final List<Livro> livros;
    private final OnClick onClick;
    private final OnLongClick onLongClick;

    public LivroAdapter(List<Livro> livros, OnClick onClick, OnLongClick onLongClick) {
        this.livros = livros;
        this.onClick = onClick;
        this.onLongClick = onLongClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_livro, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Livro livro = livros.get(position);
        h.tvTitulo.setText(livro.isFavorito() ? livro.getTitulo() + "  *" : livro.getTitulo());
        h.tvAutor.setText("Autor: " + livro.getAutor());
        h.tvInfo.setText(livro.getGenero() + " - " + livro.getAno());
        h.tvStatus.setText("Status: " + livro.getStatus());

        h.itemView.setOnClickListener(v -> onClick.onClick(livro));
        h.itemView.setOnLongClickListener(v -> {
            onLongClick.onLongClick(livro);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return livros.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvAutor, tvInfo, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvAutor = itemView.findViewById(R.id.tvAutor);
            tvInfo = itemView.findViewById(R.id.tvInfo);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
