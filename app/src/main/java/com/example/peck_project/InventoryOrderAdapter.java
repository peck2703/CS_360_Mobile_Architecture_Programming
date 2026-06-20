package com.example.peck_project;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;

public class InventoryOrderAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<InventoryItem> itemList;
    private HashMap<String, Integer> orderQuantitiesMap = new HashMap<>();

    public InventoryOrderAdapter(Context context, ArrayList<InventoryItem> itemList) {
        this.context = context;
        this.itemList = itemList;
        for (InventoryItem item : itemList) {
            orderQuantitiesMap.put(item.getItemNumber(), 0);
        }
    }

    public HashMap<String, Integer> getOrderQuantitiesMap() { return orderQuantitiesMap; }

    @Override
    public int getCount() { return itemList.size(); }

    @Override
    public Object getItem(int position) { return itemList.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_inventory_card, parent, false);
        }

        InventoryItem item = itemList.get(position);

        ImageView ivPicture = convertView.findViewById(R.id.inventory_item_picture);
        TextView tvName = convertView.findViewById(R.id.inventory_item_name);
        TextView tvNum = convertView.findViewById(R.id.inventory_item_num);
        TextView tvQty = convertView.findViewById(R.id.inventory_item_qty);
        TextView tvDesc = convertView.findViewById(R.id.inventory_item_desc);

        tvName.setText(item.getName());
        tvNum.setText("SKU: " + item.getItemNumber());
        tvQty.setText("Available: " + item.getQuantity());
        tvDesc.setText(item.getDescription());

        if (item.getImageBytes() != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(item.getImageBytes(), 0, item.getImageBytes().length);
            ivPicture.setImageBitmap(bitmap);
        }

        LinearLayout quantityControls = convertView.findViewById(R.id.inventory_quantity_controls);
        quantityControls.setVisibility(View.VISIBLE);

        Button btnMinus = convertView.findViewById(R.id.btn_card_minus);
        Button btnPlus = convertView.findViewById(R.id.btn_card_plus);
        EditText etCardQty = convertView.findViewById(R.id.et_card_qty);

        etCardQty.setTag(null);
        int currentOrderAmount = orderQuantitiesMap.get(item.getItemNumber());
        etCardQty.setText(String.valueOf(currentOrderAmount));
        etCardQty.setTag(item.getItemNumber());

        btnPlus.setOnClickListener(v -> {
            int currentVal = orderQuantitiesMap.get(item.getItemNumber());
            int newVal = currentVal + 1;
            orderQuantitiesMap.put(item.getItemNumber(), newVal);
            etCardQty.setText(String.valueOf(newVal));
        });

        btnMinus.setOnClickListener(v -> {
            int currentVal = orderQuantitiesMap.get(item.getItemNumber());
            if (currentVal > 0) {
                int newVal = currentVal - 1;
                orderQuantitiesMap.put(item.getItemNumber(), newVal);
                etCardQty.setText(String.valueOf(newVal));
            }
        });

        etCardQty.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String currentTag = (String) etCardQty.getTag();
                if (currentTag != null && currentTag.equals(item.getItemNumber())) {
                    try {
                        int parsedValue = Integer.parseInt(s.toString());
                        orderQuantitiesMap.put(item.getItemNumber(), parsedValue);
                    } catch (NumberFormatException e) {
                        orderQuantitiesMap.put(item.getItemNumber(), 0);
                    }
                }
            }
        });

        return convertView;
    }
}
