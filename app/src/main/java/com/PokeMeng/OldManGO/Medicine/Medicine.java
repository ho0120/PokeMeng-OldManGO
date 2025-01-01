package com.PokeMeng.OldManGO.Medicine;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class Medicine implements Parcelable {
    private String name;
    private String frequency;
    private ArrayList<String> times;
    private String dosage;
    private int stock;
    private int stock2;
    private String imageUrl;
    private String spinner2Value;
    private int id;
    private String startDate;
    private boolean isTaken;
    private String takenDate;
    private boolean deleted; // 新增的删除状态属性

    // Constructor
    public Medicine(String name, String frequency, ArrayList<String> times, String dosage, int stock, int stock2, String imageUrl, String spinner2Value, int id, String startDate) {
        this.name = name;
        this.frequency = frequency;
        this.times = times;
        this.dosage = dosage;
        this.stock = stock;
        this.stock2 = stock2;
        this.imageUrl = imageUrl;
        this.spinner2Value = spinner2Value;
        this.id = id;
        this.startDate = startDate;
        this.isTaken = false; // 默认未服用
        this.deleted = false; // 默认未删除
    }



    public Medicine() {
        // 需要的无参构造函数
    }

    protected Medicine(Parcel in) {
        name = in.readString();
        frequency = in.readString();
        times = in.createStringArrayList();
        dosage = in.readString();
        stock = in.readInt();
        stock2 = in.readInt();
        imageUrl = in.readString();
        spinner2Value = in.readString();
        id = in.readInt();
        startDate = in.readString();
        isTaken = in.readByte() != 0; // 从 Parcel 中读取是否已服用
        deleted = in.readByte() != 0; // 从 Parcel 中读取删除状态
    }



    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public ArrayList<String> getTimes() {
        return times;
    }

    public void setTimes(ArrayList<String> times) {
        this.times = times;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getStock2() {
        return stock2;
    }

    public void setStock2(int stock2) {
        this.stock2 = stock2;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getSpinner2Value() {
        return spinner2Value;
    }

    public void setSpinner2Value(String spinner2Value) {
        this.spinner2Value = spinner2Value;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public boolean isTaken() {
        return isTaken;
    }

    public void setTaken(boolean taken) {
        isTaken = taken;
    }

    public String getTakenDate() {
        return takenDate;
    }

    public void setTakenDate(String takenDate) {
        this.takenDate = takenDate;
    }

    // 新增的删除状态方法
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    // Method to update medicine details
    public void update(Medicine newMedicine) {
        this.name = newMedicine.getName();
        this.frequency = newMedicine.getFrequency();
        this.times = newMedicine.getTimes();
        this.dosage = newMedicine.getDosage();
        this.stock = newMedicine.getStock();
        this.stock2 = newMedicine.getStock2();
        this.spinner2Value = newMedicine.getSpinner2Value();
        this.startDate = newMedicine.getStartDate();
    }



    public static final Parcelable.Creator<Medicine> CREATOR = new Parcelable.Creator<Medicine>() {
        @Override
        public Medicine createFromParcel(Parcel in) {
            return new Medicine(in);
        }

        @Override
        public Medicine[] newArray(int size) {
            return new Medicine[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(frequency);
        dest.writeStringList(times);
        dest.writeString(dosage);
        dest.writeInt(stock);
        dest.writeInt(stock2);
        dest.writeString(imageUrl);
        dest.writeString(spinner2Value);
        dest.writeInt(id);
        dest.writeString(startDate);
        dest.writeByte((byte) (isTaken ? 1 : 0)); // 写入是否服用
        dest.writeByte((byte) (deleted ? 1 : 0)); // 写入删除状态
    }
}
