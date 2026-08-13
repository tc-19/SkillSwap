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
import com.google.firebase.firestore.WriteBatch;
import com.tubacelik.myapp.databinding.FragmentRequestsBinding;

import java.util.ArrayList;
import java.util.List;

public class RequestsFragment extends Fragment {

    private FragmentRequestsBinding binding;

    private FirebaseFirestore database;
    private FirebaseUser currentUser;

    /*
     * allRequests enthält alle Anfragen,
     * an denen der Nutzer beteiligt ist.
     */
    private final List<RequestItem> allRequests =
            new ArrayList<>();

    /*
     * visibleRequests enthält nur die aktuell
     * ausgewählte Ansicht: Gesendet oder Erhalten.
     */
    private final List<RequestItem> visibleRequests =
            new ArrayList<>();

    private RequestAdapter adapter;

    private boolean showingSentRequests = true;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentRequestsBinding.inflate(
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

        adapter = new RequestAdapter(
                visibleRequests,
                currentUser.getUid(),
                new RequestAdapter
                        .RequestActionListener() {

                    @Override
                    public void onAccept(
                            RequestItem request,
                            int position
                    ) {
                        acceptRequest(request);
                    }

                    @Override
                    public void onReject(
                            RequestItem request,
                            int position
                    ) {
                        rejectRequest(request);
                    }

                    @Override
                    public void onOpenAcceptedSkill(
                            RequestItem request
                    ) {
                        openSkillForRating(request);
                    }
                }
        );

        binding.requestsRecyclerView
                .setLayoutManager(
                        new LinearLayoutManager(
                                requireContext()
                        )
                );

        binding.requestsRecyclerView
                .setAdapter(adapter);

        binding.sentRequestsButton
                .setOnClickListener(v -> {
                    showingSentRequests = true;
                    showFilteredRequests();
                });

        binding.receivedRequestsButton
                .setOnClickListener(v -> {
                    showingSentRequests = false;
                    showFilteredRequests();
                });

        loadRequests();
    }

    /*
     * Lädt alle Requests und behält nur diejenigen,
     * bei denen der aktuelle Nutzer Sender oder
     * Empfänger ist.
     */
    private void loadRequests() {
        binding.requestsLoadingProgress
                .setVisibility(View.VISIBLE);

        database.collection("requests")
                .get()
                .addOnSuccessListener(result -> {
                    allRequests.clear();

                    for (DocumentSnapshot document
                            : result.getDocuments()) {

                        RequestItem request =
                                document.toObject(
                                        RequestItem.class
                                );

                        if (request == null) {
                            continue;
                        }

                        boolean userIsSender =
                                currentUser.getUid()
                                        .equals(
                                                request
                                                        .getSenderId()
                                        );

                        boolean userIsReceiver =
                                currentUser.getUid()
                                        .equals(
                                                request
                                                        .getReceiverId()
                                        );

                        if (userIsSender
                                || userIsReceiver) {

                            allRequests.add(request);
                        }
                    }

                    binding.requestsLoadingProgress
                            .setVisibility(View.GONE);

                    showFilteredRequests();
                })
                .addOnFailureListener(error -> {
                    binding.requestsLoadingProgress
                            .setVisibility(View.GONE);

                    showMessage(
                            "Anfragen konnten nicht "
                                    + "geladen werden: "
                                    + error.getMessage()
                    );
                });
    }

    /*
     * Zeigt abhängig vom ausgewählten Button entweder
     * gesendete oder erhaltene Anfragen.
     */
    private void showFilteredRequests() {
        visibleRequests.clear();

        for (RequestItem request : allRequests) {
            boolean belongsToCurrentList;

            if (showingSentRequests) {
                belongsToCurrentList =
                        currentUser.getUid()
                                .equals(
                                        request.getSenderId()
                                );
            } else {
                belongsToCurrentList =
                        currentUser.getUid()
                                .equals(
                                        request.getReceiverId()
                                );
            }

            if (belongsToCurrentList) {
                visibleRequests.add(request);
            }
        }

        adapter.notifyDataSetChanged();

        binding.requestsSubtitleText.setText(
                showingSentRequests
                        ? "Anfragen, die du an "
                        + "andere gesendet hast."
                        : "Anfragen, die du "
                        + "erhalten hast."
        );

        binding.sentRequestsButton.setEnabled(
                !showingSentRequests
        );

        binding.receivedRequestsButton.setEnabled(
                showingSentRequests
        );

        binding.noRequestsText.setText(
                showingSentRequests
                        ? "Du hast noch keine "
                        + "Anfragen gesendet."
                        : "Du hast noch keine "
                        + "Anfragen erhalten."
        );

        binding.noRequestsText.setVisibility(
                visibleRequests.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    /*
     * Öffnet den Skill-Detail-Screen.
     * Dort erkennt SkillDetailFragment anhand des
     * angenommenen Requests, dass bewertet werden darf.
     */
    private void openSkillForRating(
            RequestItem request
    ) {
        if (request.getSkillId() == null
                || request.getSkillId()
                .trim()
                .isEmpty()) {

            showMessage(
                    "Der zugehörige Skill "
                            + "wurde nicht gefunden."
            );
            return;
        }

        Bundle bundle = new Bundle();

        bundle.putString(
                "skillId",
                request.getSkillId()
        );

        NavHostFragment.findNavController(this)
                .navigate(
                        R.id.action_RequestsFragment_to_SkillDetailFragment,
                        bundle
                );
    }

    /*
     * Wenn eine Anfrage angenommen wird:
     *
     * 1. ausgewählte Anfrage = angenommen
     * 2. alle anderen offenen Anfragen für denselben
     *    Skill = abgelehnt
     * 3. Skill erhält availability = vergeben
     *
     * Alle Änderungen werden gemeinsam als Batch
     * gespeichert.
     */
    private void acceptRequest(
            RequestItem acceptedRequest
    ) {
        if (acceptedRequest.getSkillId() == null
                || acceptedRequest.getRequestId() == null) {

            showMessage(
                    "Anfrage enthält ungültige Daten."
            );
            return;
        }

        database.collection("requests")
                .whereEqualTo(
                        "skillId",
                        acceptedRequest.getSkillId()
                )
                .get()
                .addOnSuccessListener(result -> {
                    WriteBatch batch =
                            database.batch();

                    for (DocumentSnapshot document
                            : result.getDocuments()) {

                        String currentStatus =
                                document.getString(
                                        "status"
                                );

                        if (document.getId().equals(
                                acceptedRequest
                                        .getRequestId()
                        )) {
                            batch.update(
                                    document.getReference(),
                                    "status",
                                    "angenommen"
                            );

                        } else if (isOpenStatus(
                                currentStatus
                        )) {
                            /*
                             * Andere offene Interessenten werden
                             * automatisch abgelehnt.
                             */
                            batch.update(
                                    document.getReference(),
                                    "status",
                                    "abgelehnt"
                            );
                        }
                    }

                    /*
                     * Der Skill wird als vergeben markiert.
                     * Dadurch verschwindet er aus Entdecken.
                     */
                    batch.update(
                            database.collection("skills")
                                    .document(
                                            acceptedRequest
                                                    .getSkillId()
                                    ),
                            "availability",
                            "vergeben"
                    );

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                updateLocalRequestStates(
                                        acceptedRequest
                                                .getRequestId(),
                                        acceptedRequest
                                                .getSkillId()
                                );

                                showFilteredRequests();

                                showMessage(
                                        "Anfrage wurde angenommen. "
                                                + "Der Skill ist nun "
                                                + "vergeben."
                                );
                            })
                            .addOnFailureListener(error ->
                                    showMessage(
                                            "Anfrage konnte nicht "
                                                    + "angenommen werden: "
                                                    + error.getMessage()
                                    )
                            );
                })
                .addOnFailureListener(error ->
                        showMessage(
                                "Zugehörige Anfragen konnten "
                                        + "nicht geladen werden: "
                                        + error.getMessage()
                        )
                );
    }

    /*
     * Aktualisiert nach erfolgreichem Batch auch
     * die Objekte der aktuell sichtbaren App-Liste.
     */
    private void updateLocalRequestStates(
            String acceptedRequestId,
            String skillId
    ) {
        for (RequestItem request : allRequests) {
            if (!skillId.equals(
                    request.getSkillId()
            )) {
                continue;
            }

            if (acceptedRequestId.equals(
                    request.getRequestId()
            )) {
                request.setStatus("angenommen");

            } else if (isOpenStatus(
                    request.getStatus()
            )) {
                request.setStatus("abgelehnt");
            }
        }
    }

    /*
     * Eine einzelne Anfrage ablehnen.
     */
    private void rejectRequest(
            RequestItem request
    ) {
        database.collection("requests")
                .document(request.getRequestId())
                .update(
                        "status",
                        "abgelehnt"
                )
                .addOnSuccessListener(unused -> {
                    request.setStatus("abgelehnt");

                    showFilteredRequests();

                    showMessage(
                            "Anfrage wurde abgelehnt."
                    );
                })
                .addOnFailureListener(error ->
                        showMessage(
                                "Anfrage konnte nicht "
                                        + "abgelehnt werden: "
                                        + error.getMessage()
                        )
                );
    }

    /*
     * Unterstützt auch alte Testdaten mit
     * dem englischen Wert "pending".
     */
    private boolean isOpenStatus(
            String status
    ) {
        return status == null
                || "offen".equalsIgnoreCase(status)
                || "pending".equalsIgnoreCase(status);
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
