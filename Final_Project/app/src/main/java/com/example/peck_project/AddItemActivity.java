package com.example.peck_project;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AddItemActivity extends AppCompatActivity {
    private EditText etAddName, etAddNumber, etAddDescription;
    private android.widget.Button btnSubmitNewItem;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        dbHelper = new DatabaseHelper(this);

        etAddName = findViewById(R.id.et_add_name);
        etAddNumber = findViewById(R.id.et_add_number);
        etAddDescription = findViewById(R.id.et_add_desc);
        btnSubmitNewItem = findViewById(R.id.btn_add_item_submit_item);

        btnSubmitNewItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String itemName = etAddName.getText().toString().trim();
                String itemNumber = etAddNumber.getText().toString().trim();
                String itemDesc = etAddDescription.getText().toString().trim();

                if (itemName.isEmpty()) {
                    etAddName.setError("Item name is required");
                    return;
                }
                if (itemNumber.isEmpty()) {
                    etAddNumber.setError("Item number is required");
                    return;
                }
                if (itemDesc.isEmpty()) {
                    etAddDescription.setError("Description is required");
                    return;
                }

                int initialQuantity = 0;
                byte[] blankPicture = null;

                boolean isInserted = dbHelper.addItem(itemName, itemNumber, initialQuantity, itemDesc, blankPicture);

                if (isInserted) {
                    Toast.makeText(AddItemActivity.this, "Item added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    etAddNumber.setError("This item number already exists!");
                    Toast.makeText(AddItemActivity.this, "Failed to add item.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}