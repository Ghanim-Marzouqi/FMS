package om.omantowerco.fms;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    // Declaration
    private ImageView ivBack;
    private EditText etEmail;
    private Button btnSend;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialization
        ivBack = findViewById(R.id.iv_back);
        etEmail = findViewById(R.id.et_email);
        btnSend = findViewById(R.id.btn_send);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Click Listeners
        ivBack.setOnClickListener(view -> {
            finish();
        });

        btnSend.setOnClickListener(view -> {
            String email = etEmail.getText().toString().trim();

            // Validate input
            if (!isValidInput(email))
                return;

            auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful())
                    Toast.makeText(ForgotPasswordActivity.this, "Email sent. Please check your inbox to reset password.", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(ForgotPasswordActivity.this, "This email has not been registered!", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private boolean isValidInput(String email) {

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter email address", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, "Entered email address is invalid", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }
}