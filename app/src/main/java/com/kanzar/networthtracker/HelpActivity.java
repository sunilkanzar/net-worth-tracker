package com.kanzar.networthtracker;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.kanzar.networthtracker.databinding.ActivityHelpBinding;
import com.kanzar.networthtracker.helpers.Tools;

public class HelpActivity extends AppCompatActivity {

    private ActivityHelpBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHelpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupContent();
        binding.backBtn.setOnClickListener(v -> finish());
    }

    private void setupContent() {
        int accentColor = ContextCompat.getColor(this, Tools.getAccentColor());
        binding.featuresTitle.setTextColor(accentColor);
        binding.hiddenFeaturesTitle.setTextColor(accentColor);
        binding.faqTitle.setTextColor(accentColor);

        // Features
        binding.feature1.featureText.setText(getString(R.string.feature_1));
        binding.feature2.featureText.setText(getString(R.string.feature_2));
        binding.feature3.featureText.setText(getString(R.string.feature_3));

        // FAQs
        binding.faq1.questionText.setText(getString(R.string.faq_q1));
        binding.faq1.answerText.setText(getString(R.string.faq_a1));

        binding.faq2.questionText.setText(getString(R.string.faq_q2));
        binding.faq2.answerText.setText(getString(R.string.faq_a2));

        binding.faq3.questionText.setText(getString(R.string.faq_q3));
        binding.faq3.answerText.setText(getString(R.string.faq_a3));
    }
}
