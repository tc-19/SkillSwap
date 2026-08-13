package com.tubacelik.myapp;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tubacelik.myapp.databinding.FragmentFirstBinding;

import java.util.HashMap;
import java.util.Map;

public class FirstFragment extends Fragment {


    private FragmentFirstBinding binding;

    // false = Login-Ansicht, true = Registrierungsansicht
    private boolean registerMode = false;


    private FirebaseAuth auth;

    private FirebaseFirestore database;

    //erstellt die Benutzeroberfläche des Fragments
    //indem xml Datei geladen wird
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, //inflater wird nie null sein
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot(); // gibt fertige Oberfläche an Android zurück
    }

    //initialisiert Firebase und richtet Button-Klicks ein
    @Override
    public void onViewCreated(
            @NonNull View view,
            Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();

        // Login-Button:
        // Im Login-Modus wird eingeloggt
        // Im Register-Modus wechselt er zurück zur Login-Ansicht.
        binding.loginButton.setOnClickListener(v -> {
            if (registerMode) {
                showLoginMode();
            } else {
                loginUser();
            }
        });

        // Register-Button:
        // Beim ersten Klick wird die Registrierungsansicht geöffnet.
        // Im Register-Modus wird anschließend der Account erstellt.
        binding.registerButton.setOnClickListener(v -> {
            if (registerMode) {
                registerUser();
            } else {
                showRegisterMode();
            }
        });
    }

    /**
     * Zeigt Name und Passwortbestätigung für die Registrierung an.
     */
    private void showRegisterMode() {
        registerMode = true;

        binding.nameInput.setVisibility(View.VISIBLE);
        binding.confirmPasswordInput.setVisibility(View.VISIBLE);

        binding.loginButton.setText("Already have an account? Login");
        binding.registerButton.setText("Register");
    }

    /**
     * Blendet die zusätzlichen Registrierungsfelder wieder aus.
     */
    private void showLoginMode() {
        registerMode = false;

        binding.nameInput.setVisibility(View.GONE); //blendet Namensfeld aus
        binding.confirmPasswordInput.setVisibility(View.GONE);

        binding.loginButton.setText("Login");
        binding.registerButton.setText("No account? Register here");
    }

    /**
     * Prüft die Eingaben und erstellt einen Account
     * mit Firebase Authentication.
     */
    private void registerUser() {
        String name = binding.nameInput.getText().toString().trim();
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString();
        String confirmPassword =
                binding.confirmPasswordInput.getText().toString();

        if (name.isEmpty()
                || email.isEmpty()
                || password.isEmpty()
                || confirmPassword.isEmpty()) {

            showMessage("Bitte alle Felder ausfüllen.");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMessage("Bitte eine gültige E-Mail-Adresse eingeben.");
            return;
        }

        if (password.length() < 6) {
            showMessage("Das Passwort muss mindestens 6 Zeichen haben.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showMessage("Die Passwörter stimmen nicht überein.");
            return;
        }

        setButtonsEnabled(false); //deaktiviert beide Buttons

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> { //wird ausgeführt, sobald Firebase mit Register fertig ist
                    if (!task.isSuccessful()) {
                        setButtonsEnabled(true);

                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Unbekannter Fehler";

                        showMessage(
                                "Registrierung fehlgeschlagen: "
                                        + errorMessage
                        );
                        return;
                    }

                    FirebaseUser currentUser = auth.getCurrentUser();

                    if (currentUser == null) {
                        setButtonsEnabled(true);
                        showMessage("Der Benutzer konnte nicht geladen werden.");
                        return;
                    }

                    saveUserProfile(
                            currentUser.getUid(),
                            name,
                            email
                    );
                });
    }

    /**
     * Speichert zusätzliche Profildaten in Firestore.
     * Das Passwort wird nicht in Firestore gespeichert.
     */
    private void saveUserProfile(
            String userId,
            String name,
            String email
    ) {
        Map<String, Object> userProfile = new HashMap<>();

        userProfile.put("userId", userId);
        userProfile.put("name", name);
        userProfile.put("email", email);

        database.collection("users")
                .document(userId)// wählt das Dokument mit Benutzer-id aus
                .set(userProfile)
                .addOnSuccessListener(unused -> {
                    setButtonsEnabled(true);
                    showMessage("Registrierung erfolgreich.");
                    openHomeScreen();
                })
                .addOnFailureListener(error -> {
                    setButtonsEnabled(true);

                    showMessage(
                            "Das Profil konnte nicht gespeichert werden: "
                                    + error.getMessage()
                    );
                });
    }

    /**
     * Meldet einen bestehenden Benutzer mit E-Mail und Passwort an.
     */
    private void loginUser() {
        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Bitte E-Mail und Passwort eingeben.");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMessage("Bitte eine gültige E-Mail-Adresse eingeben.");
            return;
        }

        setButtonsEnabled(false);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    setButtonsEnabled(true);

                    if (task.isSuccessful()) {
                        showMessage("Login erfolgreich.");
                        openHomeScreen();
                    } else {
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Unbekannter Fehler";

                        showMessage(
                                "Login fehlgeschlagen: "
                                        + errorMessage
                        );
                    }
                });
    }

    /**
     * Verhindert mehrfache Klicks während einer Firebase-Anfrage.
     */
    private void setButtonsEnabled(boolean enabled) {
        binding.loginButton.setEnabled(enabled);
        binding.registerButton.setEnabled(enabled);
    }

    /**
     * Öffnet den Home-Screen.
     */
    private void openHomeScreen() {
        NavHostFragment.findNavController(this)//holt den NavController des fragments
                .navigate(R.id.action_FirstFragment_to_SecondFragment);
    }

    /**
     * Zeigt eine kurze Meldung auf dem Bildschirm.
     */
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
        binding = null; //löscht binding, damit speicher freigegeben wird
    }
}