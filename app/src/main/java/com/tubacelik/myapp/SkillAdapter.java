package com.tubacelik.myapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tubacelik.myapp.databinding.ItemSkillBinding;

import java.util.List;

// Adapter zum Anzeigen der Skills in der RecyclerView
public class SkillAdapter
        extends RecyclerView.Adapter<SkillAdapter.SkillViewHolder> {

    // reagiert auf einen Klick auf einen Skill (Browse-Screen)
    public interface OnSkillClickListener {
        void onSkillClick(Skill skill);
    }

    // reagiert auf Bearbeiten und Löschen (Meine Skills)
    public interface ManageSkillListener {
        void onEdit(Skill skill);
        void onDelete(Skill skill, int position);
    }

    private final List<Skill> skills;
    private final OnSkillClickListener clickListener;
    private final ManageSkillListener manageListener;

    // Für den Browse-Screen
    public SkillAdapter(
            List<Skill> skills,
            OnSkillClickListener clickListener
    ) {
        this.skills = skills;
        this.clickListener = clickListener;
        this.manageListener = null;
    }

    // Für den Meine-Skills-Screen
    public SkillAdapter(
            List<Skill> skills,
            ManageSkillListener manageListener
    ) {
        this.skills = skills;
        this.clickListener = null;
        this.manageListener = manageListener;
    }

    @NonNull
    @Override
    public SkillViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemSkillBinding binding = ItemSkillBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );

        return new SkillViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull SkillViewHolder holder,
            int position
    ) {
        holder.bind(skills.get(position), position);
    }

    @Override
    public int getItemCount() {
        return skills.size();
    }

    class SkillViewHolder extends RecyclerView.ViewHolder {

        private final ItemSkillBinding binding;

        SkillViewHolder(ItemSkillBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Skill skill, int position) {
            binding.skillTitleText.setText(skill.getTitle());
            binding.skillCategoryText.setText(skill.getCategory());
            binding.skillDescriptionText.setText(skill.getDescription());
            binding.skillLevelText.setText(
                    "Niveau: " + skill.getLevel()
            );

            String owner = skill.getOwnerName();

            if (owner == null || owner.trim().isEmpty()) {
                owner = "Unbekannt";
            }

            binding.skillOwnerText.setText("von " + owner);

            boolean managementMode = manageListener != null;

            binding.skillActionsLayout.setVisibility(
                    managementMode ? View.VISIBLE : View.GONE
            );

            // Bei eigenen Skills brauchen wir den Besitzertext nicht.
            binding.skillOwnerText.setVisibility(
                    managementMode ? View.GONE : View.VISIBLE
            );

            if (clickListener != null) {
                binding.getRoot().setOnClickListener(v ->
                        clickListener.onSkillClick(skill)
                );
            } else {
                binding.getRoot().setOnClickListener(null);
            }

            if (managementMode) {
                binding.editSkillButton.setOnClickListener(v ->
                        manageListener.onEdit(skill)
                );

                binding.deleteSkillButton.setOnClickListener(v ->
                        manageListener.onDelete(skill, position)
                );
            }
        }
    }
}
