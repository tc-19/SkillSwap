package com.tubacelik.myapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tubacelik.myapp.databinding.FragmentSkillDetailBinding;

import java.util.HashMap;
import java.util.Map;

public class SkillDetailFragment extends Fragment {

    private FragmentSkillDetailBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore database;

    private Skill selectedSkill;
    private FirebaseUser currentUser;
    private boolean requestAlreadyExists = false;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSkillDetailBinding.inflate(
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
        currentUser = auth.getCurrentUser();

        binding.sendRequestButton.setEnabled(false);
        binding.ratingSection.setVisibility(View.GONE);

        String skillId = getArguments() != null
                ? getArguments().getString("skillId")
                : null;

        if (skillId == null || skillId.trim().isEmpty()) {
            showMessage("Skill konnte nicht geöffnet werden.");
            return;
        }

        binding.sendRequestButton.setOnClickListener(v ->
                prepareRequest()
        );

        binding.submitRatingButton.setOnClickListener(v ->
                prepareRating()
        );

        loadSkill(skillId);
    }

    private void loadSkill(String skillId) {
        binding.detailLoadingProgress.setVisibility(View.VISIBLE);

        database.collection("skills")
                .document(skillId)
                .get()
                .addOnSuccessListener(document -> {
                    binding.detailLoadingProgress.setVisibility(
                            View.GONE
                    );

                    selectedSkill =
                            document.toObject(Skill.class);

                    if (selectedSkill == null) {
                        showMessage("Skill wurde nicht gefunden.");
                        return;
                    }

                    displaySkill();
                    checkExistingRequest();
                    loadRatings();
                })
                .addOnFailureListener(error -> {
                    binding.detailLoadingProgress.setVisibility(
                            View.GONE
                    );

                    showMessage(
                            "Skill konnte nicht geladen werden: "
                                    + error.getMessage()
                    );
                });
    }

    private void displaySkill() {
        binding.detailTitleText.setText(
                selectedSkill.getTitle()
        );

        binding.detailCategoryText.setText(
                selectedSkill.getCategory()
        );

        binding.detailOwnerText.setText(
                "Angeboten von "
                        + selectedSkill.getOwnerName()
        );

        binding.detailLevelText.setText(
                "Kenntnisstand: "
                        + selectedSkill.getLevel()
        );

        binding.detailDescriptionText.setText(
                selectedSkill.getDescription()
        );

        boolean ownSkill = currentUser != null
                && currentUser.getUid().equals(
                selectedSkill.getOwnerId()
        );

        binding.requestSection.setVisibility(
                ownSkill ? View.GONE : View.VISIBLE
        );

        binding.ratingSection.setVisibility(View.GONE);
    }

    private void checkExistingRequest() {
        if (currentUser == null || selectedSkill == null) {
            return;
        }

        if (currentUser.getUid().equals(
                selectedSkill.getOwnerId()
        )) {
            return;
        }

        binding.sendRequestButton.setEnabled(false);
        binding.sendRequestButton.setText(
                "Anfrage wird geprüft..."
        );

        database.collection("requests")
                .whereEqualTo(
                        "skillId",
                        selectedSkill.getSkillId()
                )
                .get()
                .addOnSuccessListener(result -> {
                    RequestItem existingRequest = null;

                    for (DocumentSnapshot document
                            : result.getDocuments()) {

                        RequestItem request =
                                document.toObject(
                                        RequestItem.class
                                );

                        if (request != null
                                && currentUser.getUid().equals(
                                request.getSenderId()
                        )) {

                            existingRequest = request;

                            if ("angenommen".equalsIgnoreCase(
                                    request.getStatus()
                            )) {
                                break;
                            }
                        }
                    }

                    applyRequestState(existingRequest);
                })
                .addOnFailureListener(error -> {
                    binding.sendRequestButton.setEnabled(true);
                    binding.sendRequestButton.setText(
                            "Anfrage senden"
                    );
                });
    }

    private void applyRequestState(
            RequestItem existingRequest
    ) {
        requestAlreadyExists = existingRequest != null;

        if (existingRequest == null) {
            binding.sendRequestButton.setEnabled(true);
            binding.sendRequestButton.setText(
                    "Anfrage senden"
            );

            binding.requestMessageInput.setEnabled(true);
            binding.ratingSection.setVisibility(View.GONE);
            return;
        }

        binding.sendRequestButton.setEnabled(false);
        binding.requestMessageInput.setEnabled(false);

        String status = existingRequest.getStatus();

        if ("pending".equalsIgnoreCase(status)) {
            status = "offen";
        }

        if ("angenommen".equalsIgnoreCase(status)) {
            binding.sendRequestButton.setText(
                    "Anfrage wurde angenommen"
            );

            binding.ratingSection.setVisibility(View.VISIBLE);
            binding.submitRatingButton.setEnabled(true);
        } else if ("abgelehnt".equalsIgnoreCase(status)) {
            binding.sendRequestButton.setText(
                    "Anfrage wurde abgelehnt"
            );

            binding.ratingSection.setVisibility(View.GONE);
        } else {
            binding.sendRequestButton.setText(
                    "Anfrage ist noch offen"
            );

            binding.ratingSection.setVisibility(View.GONE);
        }
    }

    private void prepareRequest() {
        if (currentUser == null || selectedSkill == null) {
            showMessage(
                    "Anfrage konnte nicht gesendet werden."
            );
            return;
        }

        if (requestAlreadyExists) {
            showMessage(
                    "Du hast diesen Skill bereits angefragt."
            );
            return;
        }

        String message = binding.requestMessageInput
                .getText()
                .toString()
                .trim();

        if (message.isEmpty()) {
            showMessage(
                    "Bitte schreibe eine kurze Nachricht."
            );
            return;
        }

        binding.sendRequestButton.setEnabled(false);
        binding.sendRequestButton.setText(
                "Wird gesendet..."
        );

        loadUserName(
                currentUser.getUid(),
                name -> saveRequest(name, message),
                true
        );
    }

    private void saveRequest(
            String senderName,
            String message
    ) {
        String requestId =
                selectedSkill.getSkillId()
                        + "_"
                        + currentUser.getUid();

        Map<String, Object> request = new HashMap<>();

        request.put("requestId", requestId);
        request.put("skillId", selectedSkill.getSkillId());
        request.put("skillTitle", selectedSkill.getTitle());

        request.put("senderId", currentUser.getUid());
        request.put("senderName", senderName);

        request.put(
                "receiverId",
                selectedSkill.getOwnerId()
        );

        request.put(
                "receiverName",
                selectedSkill.getOwnerName()
        );

        request.put("message", message);
        request.put("status", "offen");
        request.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        database.collection("requests")
                .document(requestId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        requestAlreadyExists = true;

                        binding.sendRequestButton.setText(
                                "Skill bereits angefragt"
                        );

                        showMessage(
                                "Du hast diesen Skill bereits angefragt."
                        );
                        return;
                    }

                    writeRequest(requestId, request);
                })
                .addOnFailureListener(error ->
                        resetRequestButton()
                );
    }

    private void writeRequest(
            String requestId,
            Map<String, Object> request
    ) {
        database.collection("requests")
                .document(requestId)
                .set(request)
                .addOnSuccessListener(unused -> {
                    requestAlreadyExists = true;

                    binding.sendRequestButton.setText(
                            "Anfrage ist noch offen"
                    );

                    binding.requestMessageInput.setEnabled(false);

                    showMessage(
                            "Anfrage wurde erfolgreich gesendet."
                    );
                })
                .addOnFailureListener(error -> {
                    resetRequestButton();

                    showMessage(
                            "Anfrage konnte nicht gesendet werden: "
                                    + error.getMessage()
                    );
                });
    }

    private void resetRequestButton() {
        binding.sendRequestButton.setEnabled(true);
        binding.sendRequestButton.setText(
                "Anfrage senden"
        );
    }

    private void prepareRating() {
        if (currentUser == null || selectedSkill == null) {
            showMessage(
                    "Bewertung konnte nicht gespeichert werden."
            );
            return;
        }

        float stars = binding.ratingBar.getRating();

        if (stars < 1) {
            showMessage(
                    "Bitte wähle mindestens einen Stern."
            );
            return;
        }

        String comment = binding.ratingCommentInput
                .getText()
                .toString()
                .trim();

        binding.submitRatingButton.setEnabled(false);
        binding.submitRatingButton.setText(
                "Wird gespeichert..."
        );

        loadUserName(
                currentUser.getUid(),
                name -> saveRating(
                        name,
                        stars,
                        comment
                ),
                false
        );
    }

    private void saveRating(
            String userName,
            float stars,
            String comment
    ) {
        String ratingId =
                selectedSkill.getSkillId()
                        + "_"
                        + currentUser.getUid();

        Map<String, Object> rating = new HashMap<>();

        rating.put("ratingId", ratingId);
        rating.put("skillId", selectedSkill.getSkillId());
        rating.put("reviewerId", currentUser.getUid());
        rating.put("reviewerName", userName);
        rating.put("stars", stars);
        rating.put("comment", comment);
        rating.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        database.collection("ratings")
                .document(ratingId)
                .set(rating)
                .addOnSuccessListener(unused -> {
                    binding.submitRatingButton.setEnabled(true);
                    binding.submitRatingButton.setText(
                            "Bewertung aktualisieren"
                    );

                    showMessage(
                            "Bewertung wurde gespeichert."
                    );

                    loadRatings();
                })
                .addOnFailureListener(error -> {
                    resetRatingButton();

                    showMessage(
                            "Bewertung konnte nicht gespeichert werden: "
                                    + error.getMessage()
                    );
                });
    }

    private void loadRatings() {
        if (selectedSkill == null) {
            return;
        }

        database.collection("ratings")
                .whereEqualTo(
                        "skillId",
                        selectedSkill.getSkillId()
                )
                .get()
                .addOnSuccessListener(result -> {
                    int count = result.size();
                    StringBuilder reviews =
                            new StringBuilder();

                    for (DocumentSnapshot document
                            : result.getDocuments()) {

                        Double stars =
                                document.getDouble("stars");

                        String name =
                                document.getString(
                                        "reviewerName"
                                );

                        String comment =
                                document.getString("comment");

                        reviews.append("★ ")
                                .append(
                                        stars == null
                                                ? "–"
                                                : stars.intValue()
                                )
                                .append(" von 5");

                        if (name != null
                                && !name.trim().isEmpty()) {
                            reviews.append(" · ")
                                    .append(name);
                        }

                        if (comment != null
                                && !comment.trim().isEmpty()) {
                            reviews.append("\n")
                                    .append(comment);
                        }

                        reviews.append("\n\n");
                    }

                    if (count == 0) {
                        binding.ratingSummaryText.setText(
                                "Noch keine Bewertungen vorhanden."
                        );
                    } else if (count == 1) {
                        binding.ratingSummaryText.setText(
                                "1 Bewertung"
                        );
                    } else {
                        binding.ratingSummaryText.setText(
                                count + " Bewertungen"
                        );
                    }

                    binding.reviewsText.setText(
                            reviews.length() == 0
                                    ? "Noch keine Kommentare vorhanden."
                                    : reviews.toString().trim()
                    );
                });
    }

    private void loadUserName(
            String userId,
            NameCallback callback,
            boolean requestOperation
    ) {
        database.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    String name = document.getString("name");

                    if (name == null
                            || name.trim().isEmpty()) {
                        name = "Unbekannter Nutzer";
                    }

                    callback.onNameLoaded(name);
                })
                .addOnFailureListener(error -> {
                    if (requestOperation) {
                        resetRequestButton();
                    } else {
                        resetRatingButton();
                    }

                    showMessage(
                            "Nutzerprofil konnte nicht geladen werden."
                    );
                });
    }

    private void resetRatingButton() {
        binding.submitRatingButton.setEnabled(true);
        binding.submitRatingButton.setText(
                "Bewertung speichern"
        );
    }

    private interface NameCallback {
        void onNameLoaded(String name);
    }

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
