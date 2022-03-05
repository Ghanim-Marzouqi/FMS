package om.omantowerco.fms;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;

import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import om.omantowerco.fms.models.Issue;
import om.omantowerco.fms.models.Request;
import om.omantowerco.fms.models.User;
import om.omantowerco.fms.utils.HttpHelper;
import om.omantowerco.fms.utils.VolleySingleton;

public class FormActivity extends AppCompatActivity {

    // Declaration
    private EditText etName, etEmail, etPhone, etDate, etSiteId, etOtherIssue, etLatitude, etLongitude, etChoosePhoto;
    private Spinner spIssues;
    private ImageButton btnUpload;
    private LinearLayoutCompat llOtherIssues;
    private Button btnSubmit;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private User user;
    private Issue issue;
    private List<Issue> issues = new ArrayList<>();
    private LocationManager locationManager;
    private Uri imageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        // Initialize
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etDate = findViewById(R.id.et_date);
        etSiteId = findViewById(R.id.et_site_id);
        etOtherIssue = findViewById(R.id.et_other_issue);
        etLatitude = findViewById(R.id.et_latitude);
        etLongitude = findViewById(R.id.et_longitude);
        etChoosePhoto = findViewById(R.id.et_choose_photo);
        spIssues = findViewById(R.id.sp_issues);
        btnUpload = findViewById(R.id.btn_upload);
        llOtherIssues = findViewById(R.id.ll_other_issues);
        btnSubmit = findViewById(R.id.btn_submit);

        // Initialize Location Manager & Location Listener
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Ask user to give application location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location is not enabled", Toast.LENGTH_LONG).show();
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                double latitude = Math.round(location.getLatitude() * 100.0) / 100.0;
                double longitude = Math.round(location.getLongitude() * 100.0) / 100.0;
                etLatitude.setText(String.valueOf(latitude));
                etLongitude.setText(String.valueOf(longitude));
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
                Log.d("Location Status", "Enabled");
                LocationListener.super.onProviderEnabled(provider);
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                Log.d("Location Status", "Disabled");
                LocationListener.super.onProviderDisabled(provider);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
        });

        // Initialize Firebase Auth, Firestore & Storage
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Set current date
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String formattedDate = df.format(today);
        etDate.setText(formattedDate);

        // Get logged in user
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null) {
            finish();
        } else {
            DocumentReference docRef = db.collection("users").document(firebaseUser.getUid());
            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    assert document != null;
                    if (document.exists()) {
                        // Instantiate logged in user
                        user = document.toObject(User.class);

                        // Set ui
                        etName.setText(user.getName());
                        etEmail.setText(user.getEmail());
                        etPhone.setText(user.getPhone());
                    } else {
                        Toast.makeText(this, "User doesn't exist", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Cannot get user data", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Load Metadata
        db.collection("issues").orderBy("id").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Issue issue = document.toObject(Issue.class);
                    issues.add(issue);
                }

                ArrayAdapter spinnerAdapter = new ArrayAdapter(FormActivity.this, android.R.layout.simple_spinner_dropdown_item, issues);
                spIssues.setAdapter(spinnerAdapter);
            } else {
                Toast.makeText(FormActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Spinner Listener
        spIssues.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                issue = (Issue) adapterView.getSelectedItem();

                if (issue.getName().equals("Others")) {
                    llOtherIssues.setVisibility(View.VISIBLE);
                } else {
                    llOtherIssues.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // Activity result launcher
        ActivityResultLauncher<String> choosePhotoActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        imageUri = uri;
                        String url = uri.toString().substring(0, 30);
                        etChoosePhoto.setText(url + "...");
                    }
                });

        // Choose photo listener
        btnUpload.setOnClickListener(view -> {
            choosePhotoActivityResultLauncher.launch("image/*");
        });

        // Submit Button Listener
        btnSubmit.setOnClickListener(view -> {
            Request request = new Request();
            request.setIssueId(issue.getId());
            request.setUserId(user.getId());
            request.setUserName(etName.getText().toString());
            request.setUserEmail(etEmail.getText().toString());
            request.setUserPhone(etPhone.getText().toString());
            request.setSiteId(etSiteId.getText().toString());
            request.setDate(etDate.getText().toString());
            request.setLatitude(etLatitude.getText().toString());
            request.setLongitude(etLongitude.getText().toString());
            if (request.getIssueId() == 1000)
                request.setOtherIssues(etOtherIssue.getText().toString());

            if (!isValidInput(request))
                return;

            if (!isValidEmail(request.getUserEmail()))
                return;

            if (!isValidPhone(request.getUserPhone()))
                return;
            try {
                // 1. Upload image to Firebase Storage
                if (imageUri != null) {
                    File imageFile = new File(String.valueOf(imageUri));
                    Timestamp timestamp = new Timestamp(new Date());
                    String imageName = timestamp.getNanoseconds() + imageFile.getName();
                    StorageReference ref = storage.getReference().child("images/" + imageName);
                    UploadTask uploadTask = ref.putFile(imageUri);
                    uploadTask.continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            Toast.makeText(FormActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                        return ref.getDownloadUrl();
                    }).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            Log.i("Image Url", downloadUri.toString());
                            request.setImageUrl(downloadUri.toString());

                            // 3. Create a unique id for user request
                            String requestId = UUID.randomUUID().toString();
                            request.setId(requestId);

                            // 4. Create request reference
                            db.collection("requests").get().addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    int newRef = task1.getResult().size() + 1;
                                    request.setReferenceNumber("MYV/001/" + newRef);

                                    // 5. Store to Firebase Firestore
                                    db.collection("requests").document(request.getId()).set(request).addOnCompleteListener(t -> {
                                        if (!t.isSuccessful()) {
                                            return;
                                        }

                                        // 6. Send email
                                        sendEmail(request.getId());
                                    });
                                } else {
                                    Toast.makeText(FormActivity.this, "Cannot get registered request", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(FormActivity.this, "Cannot get image download url", Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    // 1. Create a unique id for user request
                    String requestId = UUID.randomUUID().toString();
                    request.setId(requestId);

                    // 2. Create request reference
                    db.collection("requests").get().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            int newRef = task1.getResult().size() + 1;
                            request.setReferenceNumber("MYV/001/" + newRef);

                            // 5. Store to Firebase Firestore
                            db.collection("requests").document(request.getId()).set(request).addOnCompleteListener(t -> {
                                if (!t.isSuccessful()) {
                                    return;
                                }

                                // 6. Send email
                                sendEmail(request.getId());
                            });
                        } else {
                            Toast.makeText(FormActivity.this, "Cannot get registered request", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                Toast.makeText(FormActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            auth.signOut();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void sendEmail(String requestId) {
        // Fetch request details by request id
        db.collection("requests").document(requestId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                assert document != null;
                if (document.exists()) {
                    // Instantiate logged in user
                    Request request = document.toObject(Request.class);

                    if (request == null) {
                        Toast.makeText(FormActivity.this, "Cannot retrieve request", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Get issue
                    db.collection("issues")
                            .whereEqualTo("id", request.getIssueId())
                            .get()
                            .addOnCompleteListener(t -> {
                                if (t.isSuccessful()) {
                                    request.setIssue(t.getResult().getDocuments().get(0).toObject(Issue.class));
                                    Log.d("Issue Name", request.getIssue().getName());
                                    new Thread(() -> {
                                        try {
                                            Map<String, String> params = new HashMap<>();
                                            params.put("referenceNumber", request.getReferenceNumber());
                                            params.put("name", request.getUserName());
                                            params.put("email", request.getUserEmail());
                                            params.put("phone", request.getUserPhone());
                                            params.put("siteId", request.getSiteId());
                                            params.put("location", request.getLatitude() + ", " + request.getLongitude());
                                            params.put("issue", request.getIssue().getName());
                                            params.put("otherIssues", (request.getOtherIssues() == null || request.getOtherIssues().isEmpty()) ? "N\\A" : request.getOtherIssues());
                                            params.put("date", request.getDate());
                                            params.put("imageUrl", (request.getImageUrl() == null || request.getImageUrl().isEmpty()) ? "N\\A" : request.getImageUrl());

                                            JSONObject parameters = new JSONObject(params);

                                            if (HttpHelper.isOnLine(FormActivity.this)) {
                                                JsonObjectRequest httpRequest = new JsonObjectRequest(com.android.volley.Request.Method.POST, HttpHelper.SEND_EMAIL_HTTP, parameters, response -> {
                                                    if (response != null) {
                                                        try {
                                                            Toast.makeText(FormActivity.this, response.getString("message"), Toast.LENGTH_SHORT).show();
                                                        } catch (JSONException e) {
                                                            Toast.makeText(FormActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                }, error -> {
                                                    Log.e("HTTP Error", error.toString());
                                                    Toast.makeText(FormActivity.this, "An error occurred while sending your request", Toast.LENGTH_SHORT).show();
                                                });

                                                // add request to request queue
                                                VolleySingleton.getInstance(FormActivity.this).addToRequestQueue(httpRequest);
                                            } else {
                                                Toast.makeText(this, "You're Offline. Please make sure you have internet connection.", Toast.LENGTH_SHORT).show();
                                            }
                                        } catch (Exception e) {
                                            Log.e("SendMail", e.getMessage(), e);
                                            Toast.makeText(FormActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }).start();
                                }
                            });
                } else {
                    Toast.makeText(FormActivity.this, "Request doesn't exist", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(FormActivity.this, "Cannot get request data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isValidInput(Request request) {
        if (request.getUserId().isEmpty()) {
            Toast.makeText(this, "User has not logged in", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (request.getUserName().isEmpty()) {
            Toast.makeText(this, "Please enter user name", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (request.getUserEmail().isEmpty()) {
            Toast.makeText(this, "Please enter user email", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (request.getUserPhone().isEmpty()) {
            Toast.makeText(this, "Please enter user phone", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (request.getLatitude().isEmpty()) {
            Toast.makeText(this, "Please enter latitude", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (request.getLongitude().isEmpty()) {
            Toast.makeText(this, "Please enter longitude", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (request.getSiteId().isEmpty()) {
            Toast.makeText(this, "Please enter site id", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (request.getIssueId() == 1000 && request.getOtherIssues().isEmpty()) {
            Toast.makeText(this, "Please enter other issues", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    private boolean isValidPhone(String target) {
        if (target.length() != 8) {
            Toast.makeText(this, "Phone number must be 8 digits", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!target.startsWith("7") && !target.startsWith("9")) {
            Toast.makeText(this, "Phone number must start with 7 or 9", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}