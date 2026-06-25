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

        // Low stock alert styling (less than 5 items)
        int currentStock = currentItem.getQuantity();
        if (currentStock < 5) {
            tvQty.setTextColor(android.graphics.Color.RED);
            tvQty.setText("Qty: " + currentStock + " (LOW STOCK!)");
        } else {
            tvQty.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // Default Green color
        }

        byte[] imageBytes = currentItem.getImageBytes();
        if (imageBytes != null && imageBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            ivPicture.setImageBitmap(bitmap);
        }

        return convertView;
    }
}
