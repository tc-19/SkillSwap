package com.tubacelik.myapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tubacelik.myapp.databinding.ItemRequestBinding;

import java.util.List;

public class RequestAdapter
        extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    public interface RequestActionListener {

        void onAccept(
                RequestItem request,
                int position
        );

        void onReject(
                RequestItem request,
                int position
        );

        /*
         * Wird verwendet, wenn der Sender eine angenommene
         * Anfrage antippt und den Skill bewerten möchte.
         */
        void onOpenAcceptedSkill(RequestItem request);
    }

    private final List<RequestItem> requests;
    private final String currentUserId;
    private final RequestActionListener listener;

    public RequestAdapter(
            List<RequestItem> requests,
            String currentUserId,
            RequestActionListener listener
    ) {
        this.requests = requests;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemRequestBinding binding =
                ItemRequestBinding.inflate(
                        LayoutInflater.from(
                                parent.getContext()
                        ),
                        parent,
                        false
                );

        return new RequestViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RequestViewHolder holder,
            int position
    ) {
        holder.bind(
                requests.get(position),
                position
        );
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    class RequestViewHolder
            extends RecyclerView.ViewHolder {

        private final ItemRequestBinding binding;

        RequestViewHolder(
                ItemRequestBinding binding
        ) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                RequestItem request,
                int position
        ) {
            boolean received =
                    currentUserId.equals(
                            request.getReceiverId()
                    );

            String status =
                    normalizeStatus(request.getStatus());

            binding.requestSkillTitleText.setText(
                    request.getSkillTitle()
            );

            binding.requestPersonText.setText(
                    received
                            ? "Von: "
                            + request.getSenderName()
                            : "An: "
                            + request.getReceiverName()
            );

            binding.requestMessageText.setText(
                    request.getMessage()
            );

            /*
             * Bei einer angenommenen gesendeten Anfrage
             * erhält der Nutzer einen Hinweis.
             */
            if (!received
                    && "angenommen".equals(status)) {

                binding.requestStatusText.setText(
                        "Status: Angenommen"
                                + " · Zum Bewerten antippen"
                );

            } else {
                binding.requestStatusText.setText(
                        "Status: "
                                + formatStatus(status)
                );
            }

            /*
             * Nur der Empfänger darf eine offene Anfrage
             * annehmen oder ablehnen.
             */
            boolean canAnswer =
                    received
                            && "offen".equals(status);

            binding.requestActionsLayout.setVisibility(
                    canAnswer
                            ? View.VISIBLE
                            : View.GONE
            );

            binding.acceptRequestButton
                    .setOnClickListener(v ->
                            listener.onAccept(
                                    request,
                                    position
                            )
                    );

            binding.rejectRequestButton
                    .setOnClickListener(v ->
                            listener.onReject(
                                    request,
                                    position
                            )
                    );

            /*
             * Wichtig wegen RecyclerView-Recycling:
             * Zuerst wird jede alte Klickaktion entfernt.
             */
            binding.getRoot().setOnClickListener(null);
            binding.getRoot().setClickable(false);

            /*
             * Nur eine angenommene Anfrage, die der Nutzer
             * selbst gesendet hat, öffnet den Skill.
             */
            boolean canOpenForRating =
                    !received
                            && "angenommen".equals(status);

            if (canOpenForRating) {
                binding.getRoot().setClickable(true);

                binding.getRoot().setOnClickListener(v ->
                        listener.onOpenAcceptedSkill(
                                request
                        )
                );
            }
        }

        private String normalizeStatus(
                String status
        ) {
            if (status == null) {
                return "offen";
            }

            if ("pending".equalsIgnoreCase(status)) {
                return "offen";
            }

            if ("accepted".equalsIgnoreCase(status)) {
                return "angenommen";
            }

            if ("rejected".equalsIgnoreCase(status)) {
                return "abgelehnt";
            }

            return status.toLowerCase();
        }

        private String formatStatus(
                String status
        ) {
            if ("angenommen".equals(status)) {
                return "Angenommen";
            }

            if ("abgelehnt".equals(status)) {
                return "Abgelehnt";
            }

            return "Offen";
        }
    }
}