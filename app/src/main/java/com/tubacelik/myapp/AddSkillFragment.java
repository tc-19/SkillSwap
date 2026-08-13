package com.tubacelik.myapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tubacelik.myapp.databinding.FragmentAddSkillBinding;

import java.util.HashMap;
import java.util.Map;

public class AddSkillFragment extends Fragment {

    private FragmentAddSkillBinding binding;
    private FirebaseFirestore database;
    private FirebaseAuth auth;

    private String editingSkillId;
    private String editingOwnerId;

    @Override
    //erstellt und zeigt die Benutzeroberfläche an
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentAddSkillBinding.inflate( //lade die xml-datei fragment_add_skill, erstelle sichtbare Oberfläche
                inflater,
                container,
                false
        );
        //gibt das gesamte Layout-Paket über die Root-Ansicht an das System zurück
        return binding.getRoot();
    }

    @Override
    // prüft den Bearbeitungsmodus und setzt den Klick auf den Speichern-Button
    public void onViewCreated(
            @NonNull View view,
            Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        database = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupDropdowns();

        editingSkillId = getArguments() != null
                ? getArguments().getString("skillId")
                : null;

        if (editingSkillId != null
                && !editingSkillId.trim().isEmpty()) {
            loadSkillForEditing();
        }

        binding.saveSkillButton.setOnClickListener(v ->
                validateAndSaveSkill()
        );
    }

    // erstellt Dropdown-menüs und verbindet mit Eingabefeldern
    private void setupDropdowns() {
        String[] categories = {
                "Schule und Nachhilfe",
                "Sprachen",
                "Programmierung und IT",
                "Kreativität und Design",
                "Musik",
                "Sport und Fitness",
                "Alltag und Handwerk",
                "Sonstiges"
        };

        String[] levels = {
                "Anfänger",
                "Grundkenntnisse",
                "Fortgeschritten",
                "Sehr erfahren"
        };

        ArrayAdapter<String> categoryAdapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        categories
                );

        ArrayAdapter<String> levelAdapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        levels
                );

        binding.categoryInput.setAdapter(categoryAdapter);
        binding.levelInput.setAdapter(levelAdapter);

        binding.categoryInput.setOnClickListener(v ->
                binding.categoryInput.showDropDown()
        );

        binding.levelInput.setOnClickListener(v ->
                binding.levelInput.showDropDown()
        );
    }

    // lädt einen vorhandenen Skill aus Firestore,
    // damit Nutzer bearbeiten kann
    private void loadSkillForEditing() {
        setButtonLoading("Skill wird geladen...");

        database.collection("skills")
                .document(editingSkillId)
                .get()
                .addOnSuccessListener(document -> {
                    Skill skill = document.toObject(Skill.class);

                    if (skill == null) {
                        resetButton();
                        showMessage("Skill wurde nicht gefunden.");
                        return;
                    }

                    editingOwnerId = skill.getOwnerId();

                    binding.titleInput.setText(skill.getTitle());
                    binding.categoryInput.setText(
                            skill.getCategory(),
                            false
                    );
                    binding.descriptionInput.setText(
                            skill.getDescription()
                    );
                    binding.levelInput.setText(
                            skill.getLevel(),
                            false
                    );

                    binding.saveSkillButton.setEnabled(true);
                    binding.saveSkillButton.setText(
                            "Änderungen speichern"
                    );
                })
                .addOnFailureListener(error -> {
                    resetButton();

                    showMessage(
                            "Skill konnte nicht geladen werden: "
                                    + error.getMessage()
                    );
                });
    }

    // liest alle Eingaben aus
    private void validateAndSaveSkill() {
        String title = binding.titleInput
                .getText()
                .toString()
                .trim();

        String category = binding.categoryInput
                .getText()
                .toString()
                .trim();

        String description = binding.descriptionInput
                .getText()
                .toString()
                .trim();

        String level = binding.levelInput
                .getText()
                .toString()
                .trim();

        if (title.isEmpty() //prüft ob alle Felder ausgefüllt sind
                || category.isEmpty()
                || description.isEmpty()
                || level.isEmpty()) {

            showMessage("Bitte fülle alle Felder aus.");
            return;
        }

        if (editingSkillId == null) { //entscheidet ob neuer Skill oder bearbeiten
            createSkill(title, category, description, level);
        } else {
            updateSkill(title, category, description, level);
        }
    }

    //erstellt einen neuen Skill
    //speichert zusammen mit Benutzerdaten in Firestore
    private void createSkill(
            String title,
            String category,
            String description,
            String level
    ) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            showMessage("Du bist nicht eingeloggt.");
            return;
        }

        setButtonLoading("Wird gespeichert...");

        database.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(userDocument -> {
                    String ownerName =
                            userDocument.getString("name");

                    if (ownerName == null
                            || ownerName.trim().isEmpty()) {
                        ownerName = "Unbekannter Nutzer";
                    }

                    String skillId = database
                            .collection("skills")
                            .document()
                            .getId();

                    Map<String, Object> skill = new HashMap<>();

                    skill.put("skillId", skillId);
                    skill.put("title", title);
                    skill.put("category", category);
                    skill.put("description", description);
                    skill.put("level", level);
                    skill.put("ownerId", user.getUid());
                    skill.put("ownerName", ownerName);
                    skill.put(
                            "createdAt",
                            FieldValue.serverTimestamp()
                    );

                    database.collection("skills")
                            .document(skillId)
                            .set(skill)
                            .addOnSuccessListener(unused ->
                                    finishSaving(
                                            "Skill wurde gespeichert."
                                    )
                            )
                            .addOnFailureListener(error ->
                                    showSaveError(error.getMessage())
                            );
                })
                .addOnFailureListener(error ->
                        showSaveError(error.getMessage())
                );
    }

    // aktualisiert einen bereits vorhandenen Skill
    private void updateSkill(
            String title,
            String category,
            String description,
            String level
    ) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null // wenn der angemeldete Nutzer der besitzer des Skills ist
                || editingOwnerId == null
                || !user.getUid().equals(editingOwnerId)) {

            showMessage(
                    "Du darfst diesen Skill nicht bearbeiten."
            );
            return;
        }

        setButtonLoading("Wird aktualisiert...");

        Map<String, Object> changes = new HashMap<>();

        changes.put("title", title);
        changes.put("category", category);
        changes.put("description", description);
        changes.put("level", level);

        database.collection("skills")
                .document(editingSkillId)
                .update(changes)
                .addOnSuccessListener(unused ->
                        finishSaving(
                                "Änderungen wurden gespeichert."
                        )
                )
                .addOnFailureListener(error ->
                        showSaveError(error.getMessage())
                );
    }

    // deaktiviert den Speichern-Button
    private void setButtonLoading(String text) {
        binding.saveSkillButton.setEnabled(false);
        binding.saveSkillButton.setText(text);
    }

    // aktiviert den Speichern-Button wieder
    private void resetButton() {
        if (binding == null) {
            return;
        }

        binding.saveSkillButton.setEnabled(true);

        binding.saveSkillButton.setText(
                editingSkillId == null
                        ? "Skill speichern"
                        : "Änderungen speichern"
        );
    }

    //zeigt eine Erfolgsmeldung an
    // navigiert zurück zu "Meine Skills"
    private void finishSaving(String message) {
        showMessage(message);

        if (binding == null) {
            return;
        }

        androidx.navigation.NavController navController =
                NavHostFragment.findNavController(this);

        boolean returnedToMySkills =
                navController.popBackStack(
                        R.id.MySkillsFragment,
                        false
                );

        if (!returnedToMySkills) {
            navController.navigate(
                    R.id.MySkillsFragment
            );
        }
    }

    //setzt den Button zurück
    //zeigt Fehlermeldung an
    private void showSaveError(String error) {
        resetButton();

        showMessage(
                "Speichern fehlgeschlagen: " + error
        );
    }

    //zeigt dem Nutzer eine kurze Nachricht als Toast an
    private void showMessage(String message) {
        Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    public void onDestroyView() { //sichtbare Oberfläche wird entfernt
        super.onDestroyView();
        binding = null; //Binding wird gelöscht, um Speicher freizugeben
    }
}