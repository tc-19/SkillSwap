package com.tubacelik.myapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tubacelik.myapp.databinding.FragmentSecondBinding;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore database;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSecondBinding.inflate(
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

        auth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();

        binding.welcomeText.setText("Willkommen!");

        loadCurrentUserName();

        binding.browseButton.setOnClickListener(v ->
                navigateTo(R.id.BrowseSkillsFragment)
        );

        binding.addSkillButton.setOnClickListener(v ->
                navigateTo(R.id.AddSkillFragment)
        );

        binding.requestButton.setOnClickListener(v ->
                navigateTo(R.id.RequestsFragment)
        );

        binding.profileButton.setOnClickListener(v ->
                navigateTo(R.id.MySkillsFragment)
        );

        binding.settingsButton.setOnClickListener(v ->
                logout()
        );
    }

    private void loadCurrentUserName() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            return;
        }

        database.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    String name = document.getString("name");

                    if (binding == null) {
                        return;
                    }

                    if (name == null || name.trim().isEmpty()) {
                        binding.welcomeText.setText("Willkommen!");
                    } else {
                        binding.welcomeText.setText(
                                "Willkommen, " + name + "!"
                        );
                    }
                })
                .addOnFailureListener(error -> {
                    if (binding != null) {
                        binding.welcomeText.setText("Willkommen!");
                    }
                });
    }

    private void navigateTo(int destinationId) {
        NavHostFragment.findNavController(this)
                .navigate(destinationId);
    }

    private void logout() {
        auth.signOut();

        NavOptions options = new NavOptions.Builder()
                .setPopUpTo(
                        R.id.nav_graph,
                        true
                )
                .build();

        NavHostFragment.findNavController(this)
                .navigate(
                        R.id.FirstFragment,
                        null,
                        options
                );

        Toast.makeText(
                requireContext(),
                "Du wurdest abgemeldet.",
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}