package com.example.peck_project;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import com.bumptech.glide.Glide;

public class InventoryGridAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<InventoryItem> itemList;
    private boolean isDeleteMode;
    private DatabaseHelper dbHelper;

    public InventoryGridAdapter(Context context, ArrayList<InventoryItem> itemList, boolean isDeleteMode, DatabaseHelper dbHelper) {
        this.context = context;
        this.itemList = itemList;
        this.isDeleteMode = isDeleteMode;
        this.dbHelper = dbHelper;
    }

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

        InventoryItem currentItem = itemList.get(position);

        ImageView ivPicture = convertView.findViewById(R.id.inventory_item_picture);
        TextView tvName = convertView.findViewById(R.id.inventory_item_name);
        TextView tvNum = convertView.findViewById(R.id.inventory_item_num);
        TextView tvQty = convertView.findViewById(R.id.inventory_item_qty);
        TextView tvDesc = convertView.findViewById(R.id.inventory_item_desc);

        tvName.setText(currentItem.getName());
        tvNum.setText("SKU: " + currentItem.getItemNumber());
        tvQty.setText("Qty: " + currentItem.getQuantity());
        tvDesc.setText(currentItem.getDescription());

        // Dynamic low stock alert styling using your new reorderPoint column
        int currentStock = currentItem.getQuantity();
        int alertThreshold = currentItem.reorderPoint; // Accesses your newly added column profile variable

        if (currentStock <= alertThreshold) {
            tvQty.setTextColor(android.graphics.Color.RED);
            tvQty.setText("Qty: " + currentStock + " (LOW STOCK!)");
        } else {
            tvQty.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // Default Green color
        }

        // Modern image loading loop using Glide to download the string URL path
        String imageUrl = currentItem.getItemImage();

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            // Change the inline execution from the long path to this:
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(ivPicture);

        } else {
            ivPicture.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        return convertView;
    }

}
