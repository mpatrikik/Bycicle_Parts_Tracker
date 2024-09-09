package com.example.bpt;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;

public class ManualRideAddingActivity extends AppCompatActivity {

    private ArrayList<String> bicycleList;
    private ArrayAdapter<String> adapter;
    private String userId;
    private DatabaseReference mDatabase;
    private TextView datePickerTextView;
    private TextView timePickerTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manualrideadding);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        ImageButton homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
        });

        Spinner bicycleSpinner = findViewById(R.id.bicycle_spinner);
        bicycleList = new ArrayList<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bicycleList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bicycleSpinner.setAdapter(adapter);

        datePickerTextView = findViewById(R.id.date_picker);
        timePickerTextView = findViewById(R.id.time_picker);


        // Ellenőrizzük a bejelentkezett felhasználót
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user signed in. Redirecting to login...", Toast.LENGTH_SHORT).show();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        } else {
            userId = currentUser.getUid();
            Log.d("ManualRideAdding", "User ID: " + userId);

            // Ellenőrizzük, hogy a felhasználói adat létezik-e már
            checkAndCreateUser(userId);
        }

        // Spinner esemény kezelése: új kerékpár hozzáadása ha a "+"-ra kattint
        bicycleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedBicycle = bicycleList.get(position);
                if ("Add new bicycle".equals(selectedBicycle)) {
                    showAddBicycleDialog();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }

    private void checkAndCreateUser(String userId) {
        // Ellenőrizzük, hogy a felhasználó már létezik-e
        mDatabase.child("users").child(userId).get().addOnCompleteListener(task -> {
            if (!task.getResult().exists()) {
                // Létrehozzuk a felhasználói adatokat
                UserData newUser = new UserData(userId, new ArrayList<>());
                mDatabase.child("users").child(userId).setValue(newUser)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "User data created.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to create user data.", Toast.LENGTH_SHORT).show();
                        });
            } else {
                // Ha létezik, betöltjük a kerékpárokat
                loadBicycles();
            }
        });
    }

    private void loadBicycles() {
        if (userId == null) {
            Toast.makeText(this, "User ID is null. Cannot load bicycles.", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("users").child(userId).child("bicycles").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                bicycleList.clear();
                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    String bicycleName = snapshot.getValue(String.class);
                    bicycleList.add(bicycleName);
                }
                // Hozzáadjuk a "+" opciót a végére
                bicycleList.add("Add new bicycle");
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Failed to load bicycles.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Új kerékpár hozzáadása
    private void addNewBicycleToDatabase(String bicycleName) {
        if (userId == null) {
            Toast.makeText(this, "User ID is null. Cannot add bicycle.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference bicycleRef = mDatabase.child("users").child(userId).child("bicycles").push();
        bicycleRef.setValue(bicycleName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ManualRideAddingActivity.this, "Bicycle added successfully!", Toast.LENGTH_SHORT).show();
                    bicycleList.add(bicycleName);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(ManualRideAddingActivity.this, "Failed to add bicycle.", Toast.LENGTH_SHORT).show());
    }

    private void showAddBicycleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Bicycle");

        // Input field hozzáadása
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Gombok hozzáadása
        builder.setPositiveButton("Add", (dialog, which) -> {
            String bicycleName = input.getText().toString().trim();
            if (!bicycleName.isEmpty()) {
                addNewBicycleToDatabase(bicycleName);
            } else {
                Toast.makeText(this, "Bicycle name cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    public class UserData {
        private String userId;
        private ArrayList<String> bicycles;

        public UserData(String userId, ArrayList<String> bicycles) {
            this.userId = userId;
            this.bicycles = bicycles;
        }

        public String getUserId() {
            return userId;
        }

        public ArrayList<String> getBicycles() {
            return bicycles;
        }

        public void setBicycles(ArrayList<String> bicycles) {
            this.bicycles = bicycles;
        }
    }

    public void showDatePicker(View view) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view1, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = selectedYear + "/" + (selectedMonth + 1) + "/" + selectedDay;
                    datePickerTextView.setText(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    public void showTimePicker(View view) {
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view12, selectedHour, selectedMinute) -> {
                    String selectedTime = selectedHour + ":" + String.format("%02d", selectedMinute);
                    timePickerTextView.setText(selectedTime);
                }, hour, minute, true);
        timePickerDialog.show();
    }


}
