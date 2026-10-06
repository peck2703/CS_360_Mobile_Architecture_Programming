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
import com.bumptech.glide.Glide;

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

    // 1. Create a ViewHolder structure to store view objects cleanly
    static class ViewHolder {
        ImageView ivPicture;
        TextView tvName, tvNum, tvQty, tvDesc;
        LinearLayout quantityControls;
        Button btnMinus, btnPlus;
        EditText etCardQty;
        TextWatcher currentTextWatcher; // Retains current watcher target explicitly
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_inventory_card, parent, false);

            holder = new ViewHolder();
            holder.ivPicture = convertView.findViewById(R.id.inventory_item_picture);
            holder.tvName = convertView.findViewById(R.id.inventory_item_name);
            holder.tvNum = convertView.findViewById(R.id.inventory_item_num);
            holder.tvQty = convertView.findViewById(R.id.inventory_item_qty);
            holder.tvDesc = convertView.findViewById(R.id.inventory_item_desc);
            holder.quantityControls = convertView.findViewById(R.id.inventory_quantity_controls);
            holder.btnMinus = convertView.findViewById(R.id.btn_card_minus);
            holder.btnPlus = convertView.findViewById(R.id.btn_card_plus);
            holder.etCardQty = convertView.findViewById(R.id.et_card_qty);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        InventoryItem item = itemList.get(position);

        // Map textual values cleanly to views
        holder.tvName.setText(item.getName());
        holder.tvNum.setText("SKU: " + item.getItemNumber());
        holder.tvQty.setText("Available: " + item.getQuantity());
        holder.tvDesc.setText(item.getDescription());

        String imageUrl = item.getItemImage();
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivPicture);
        } else {
            holder.ivPicture.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.quantityControls.setVisibility(View.VISIBLE);

        // Strip any old text watchers away before touching text values to break update loops
        if (holder.currentTextWatcher != null) {
            holder.etCardQty.removeTextChangedListener(holder.currentTextWatcher);
        }

        // Pull active value map properties safely
        int currentOrderAmount = orderQuantitiesMap.containsKey(item.getItemNumber()) ?
                orderQuantitiesMap.get(item.getItemNumber()) : 0;
        holder.etCardQty.setText(String.valueOf(currentOrderAmount));

        // Click listeners handle changes to map state directly, then trigger single layout updates
        holder.btnPlus.setOnClickListener(v -> {
            int currentVal = orderQuantitiesMap.containsKey(item.getItemNumber()) ? orderQuantitiesMap.get(item.getItemNumber()) : 0;
            int newVal = currentVal + 1;
            orderQuantitiesMap.put(item.getItemNumber(), newVal);
            notifyDataSetChanged(); // Updates all rows cleanly across the visual screen layout at once
        });

        holder.btnMinus.setOnClickListener(v -> {
            int currentVal = orderQuantitiesMap.containsKey(item.getItemNumber()) ? orderQuantitiesMap.get(item.getItemNumber()) : 0;
            if (currentVal > 0) {
                int newVal = currentVal - 1;
                orderQuantitiesMap.put(item.getItemNumber(), newVal);
                notifyDataSetChanged(); // Updates all rows cleanly across the visual screen layout at once
            }
        });

        // Reconstruct a single, item-locked TextWatcher target context
        holder.currentTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    String valueStr = s.toString().trim();
                    int parsedValue = valueStr.isEmpty() ? 0 : Integer.parseInt(valueStr);
                    orderQuantitiesMap.put(item.getItemNumber(), parsedValue);
                } catch (NumberFormatException e) {
                    orderQuantitiesMap.put(item.getItemNumber(), 0);
                }
            }
        };

        // Explicitly hook up the newly configured safe listener
        holder.etCardQty.addTextChangedListener(holder.currentTextWatcher);

        return convertView;
    }

}
