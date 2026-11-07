package com.slangdict.index;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Danh sách int tăng dần, gọn nhẹ (không boxing) dùng làm posting list.
 * {@link #add(int)} yêu cầu id được thêm theo thứ tự không giảm; id trùng liên tiếp bị bỏ qua.
 */
final class IntList {

    private int[] data = new int[2];
    private int size;

    void add(int value) {
        if (size > 0 && data[size - 1] == value) {
            return;
        }
        if (size == data.length) {
            data = Arrays.copyOf(data, size * 2);
        }
        data[size++] = value;
    }

    int size() {
        return size;
    }

    int get(int i) {
        return data[i];
    }

    void addTo(BitSet bits) {
        for (int i = 0; i < size; i++) {
            bits.set(data[i]);
        }
    }

    void writeTo(DataOutputStream out) throws IOException {
        out.writeInt(size);
        for (int i = 0; i < size; i++) {
            out.writeInt(data[i]);
        }
    }

    /** @param bound mọi id phải thuộc [0, bound) và độ dài tối đa là bound (kiểm tra file hỏng). */
    static IntList readFrom(DataInputStream in, int bound) throws IOException {
        int n = in.readInt();
        if (n < 0 || n > bound) {
            throw new IOException("Corrupt posting list length: " + n);
        }
        IntList list = new IntList();
        list.data = new int[Math.max(n, 2)];
        for (int i = 0; i < n; i++) {
            int v = in.readInt();
            if (v < 0 || v >= bound) {
                throw new IOException("Corrupt posting id: " + v);
            }
            list.data[i] = v;
        }
        list.size = n;
        return list;
    }
}
