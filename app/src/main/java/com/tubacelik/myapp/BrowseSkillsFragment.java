package com.tubacelik.myapp;

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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.tubacelik.myapp.databinding.FragmentBrowseSkillsBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BrowseSkillsFragment
        extends Fragment {

    private FragmentBrowseSkillsBinding binding;

    private FirebaseFirestore database;
    private FirebaseUser currentUser;

    private final List<Skill> skills =
            new ArrayList<>();

    /*
     * Enthält IDs von Skills, für die bereits
     * mindestens eine Anfrage angenommen wurde.
     */
    private final Set<String> acceptedSkillIds =
            new HashSet<>();

    private SkillAdapter adapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding =
                FragmentBrowseSkillsBinding.inflate(
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
        super.onViewCreated(
                view,
                savedInstanceState
        );

        currentUser = FirebaseAuth
                .getInstance()
                .getCurrentUser();

        if (currentUser == null) {
            showMessage(
                    "Du bist nicht eingeloggt."
            );
            return;
        }

        database =
                FirebaseFirestore.getInstance();

        adapter = new SkillAdapter(
                skills,
                this::openSkillDetails
        );

        binding.skillsRecyclerView
                .setLayoutManager(
                        new LinearLayoutManager(
                                requireContext()
                        )
                );

        binding.skillsRecyclerView
                .setAdapter(adapter);

        /*
         * Zuerst werden angenommene Requests geladen.
         * Danach erst die Skills.
         */
        loadUnavailableSkillIds();
    }

    /*
     * Sammelt die Skill-IDs aller angenommenen Requests.
     * Damit werden auch alte Skills ausgeblendet, die
     * noch kein availability-Feld besitzen.
     */
    private void loadUnavailableSkillIds() {
        binding.loadingProgress
                .setVisibility(View.VISIBLE);

        binding.emptyText
                .setVisibility(View.GONE);

        database.collection("requests")
                .whereEqualTo(
                        "status",
                        "angenommen"
                )
                .get()
                .addOnSuccessListener(result -> {
                    acceptedSkillIds.clear();

                    for (DocumentSnapshot document
                            : result.getDocuments()) {

                        String skillId =
                                document.getString(
                                        "skillId"
                                );

                        if (skillId != null
                                && !skillId
                                .trim()
                                .isEmpty()) {

                            acceptedSkillIds.add(skillId);
                        }
                    }

                    loadSkills();
                })
                .addOnFailureListener(error -> {
                    /*
                     * Falls keine angenommenen Requests
                     * geladen werden können, versuchen wir
                     * trotzdem, die Skill-Liste zu laden.
                     */
                    acceptedSkillIds.clear();
                    loadSkills();
                });
    }

    private void loadSkills() {
        database.collection("skills")
                .orderBy(
                        "createdAt",
                        Query.Direction.DESCENDING
                )
                .get()
                .addOnSuccessListener(result -> {
                    skills.clear();

                    for (DocumentSnapshot document
                            : result.getDocuments()) {

                        Skill skill =
                                document.toObject(
                                        Skill.class
                                );

                        if (skill == null
                                || skill.getOwnerId() == null
                                || skill.getSkillId() == null) {

                            continue;
                        }

                        boolean ownSkill =
                                currentUser.getUid()
                                        .equals(
                                                skill
                                                        .getOwnerId()
                                        );

                        String availability =
                                document.getString(
                                        "availability"
                                );

                        boolean markedAsAssigned =
                                "vergeben"
                                        .equalsIgnoreCase(
                                                availability
                                        );

                        boolean hasAcceptedRequest =
                                acceptedSkillIds.contains(
                                        skill.getSkillId()
                                );

                        /*
                         * Es werden nur fremde und noch
                         * verfügbare Skills angezeigt.
                         */
                        if (!ownSkill
                                && !markedAsAssigned
                                && !hasAcceptedRequest) {

                            skills.add(skill);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    binding.loadingProgress
                            .setVisibility(View.GONE);

                    binding.emptyText.setText(
                            "Zurzeit werden keine "
                                    + "verfügbaren Skills "
                                    + "anderer Nutzer angeboten."
                    );

                    binding.emptyText.setVisibility(
                            skills.isEmpty()
                                    ? View.VISIBLE
                                    : View.GONE
                    );
                })
                .addOnFailureListener(error -> {
                    binding.loadingProgress
                            .setVisibility(View.GONE);

                    showMessage(
                            "Skills konnten nicht "
                                    + "geladen werden: "
                                    + error.getMessage()
                    );
                });
    }

    private void openSkillDetails(
            Skill skill
    ) {
        if (skill.getSkillId() == null
                || skill.getSkillId()
                .trim()
                .isEmpty()) {

            showMessage(
                    "Dieser Skill besitzt "
                            + "keine gültige ID."
            );
            return;
        }

        Bundle bundle = new Bundle();

        bundle.putString(
                "skillId",
                skill.getSkillId()
        );

        NavHostFragment.findNavController(this)
                .navigate(
                        R.id.action_BrowseSkillsFragment_to_SkillDetailFragment,
                        bundle
                );
    }

    private void showMessage(
            String message
    ) {
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