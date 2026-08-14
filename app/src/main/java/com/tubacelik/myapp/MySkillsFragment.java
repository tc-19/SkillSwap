package com.tubacelik.myapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tubacelik.myapp.databinding.FragmentMySkillsBinding;

import java.util.ArrayList;
import java.util.List;

public class MySkillsFragment extends Fragment {

    private FragmentMySkillsBinding binding;
    private FirebaseFirestore database;

    private final List<Skill> skills = new ArrayList<>();
    private SkillAdapter adapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        // verbindet die XML-Datei mit diesem Fragment
        binding = FragmentMySkillsBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser user = FirebaseAuth
                .getInstance()
                .getCurrentUser();

        if (user == null) {
            showMessage("Du bist nicht eingeloggt.");
            return;
        }

        database = FirebaseFirestore.getInstance();

        // erstelltt den Adapter für die RecyclerView
        adapter = new SkillAdapter(
                skills,
                new SkillAdapter.ManageSkillListener() {
                    @Override
                    public void onEdit(Skill skill) {
                        openEditScreen(skill);
                    }

                    @Override
                    public void onDelete(
                            Skill skill,
                            int position
                    ) {
                        confirmDelete(skill, position);
                    }
                }
        );

        binding.mySkillsRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        binding.mySkillsRecyclerView.setAdapter(adapter);

        loadOwnSkills(user.getUid());
    }

    // lädt alle eigenen Skills aus Firestore
    private void loadOwnSkills(String userId) {
        binding.mySkillsLoadingProgress.setVisibility(View.VISIBLE);

        database.collection("skills")
                .whereEqualTo("ownerId", userId)
                .get()
                .addOnSuccessListener(result -> {
                    skills.clear();

                    result.getDocuments().forEach(document -> {
                        Skill skill = document.toObject(Skill.class);

                        if (skill != null) {
                            skills.add(skill);
                        }
                    });

                    adapter.notifyDataSetChanged();
                    binding.mySkillsLoadingProgress.setVisibility(View.GONE);

                    binding.noOwnSkillsText.setVisibility(
                            skills.isEmpty()
                                    ? View.VISIBLE
                                    : View.GONE
                    );
                })
                .addOnFailureListener(error -> {
                    binding.mySkillsLoadingProgress.setVisibility(View.GONE);

                    showMessage(
                            "Skills konnten nicht geladen werden: "
                                    + error.getMessage()
                    );
                });
    }

    // öffnet den Bildschirm zum Bearbeiten eines Skills
    private void openEditScreen(Skill skill) {
        Bundle bundle = new Bundle();
        bundle.putString("skillId", skill.getSkillId());

        NavHostFragment.findNavController(this)
                .navigate(
                        R.id.action_MySkillsFragment_to_AddSkillFragment,
                        bundle
                );
    }

    // Zeigt einen Bestätigungsdialog vor dem Löschen
    private void confirmDelete(Skill skill, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Skill löschen")
                .setMessage(
                        "Möchtest du „"
                                + skill.getTitle()
                                + "“ wirklich löschen?"
                )
                .setPositiveButton("Löschen", (dialog, which) ->
                        deleteSkill(skill, position)
                )
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    // Löscht den Skill aus Firestore und aktualisiert die Liste
    private void deleteSkill(Skill skill, int position) {
        database.collection("skills")
                .document(skill.getSkillId())
                .delete()
                .addOnSuccessListener(unused -> {
                    skills.remove(position);
                    adapter.notifyItemRemoved(position);

                    binding.noOwnSkillsText.setVisibility(
                            skills.isEmpty()
                                    ? View.VISIBLE
                                    : View.GONE
                    );

                    showMessage("Skill wurde gelöscht.");
                })
                .addOnFailureListener(error ->
                        showMessage(
                                "Skill konnte nicht gelöscht werden: "
                                        + error.getMessage()
                        )
                );
    }

    // Zeigt eine kurze Meldung auf dem Bildschirm an
    private void showMessage(String message) {
        Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}