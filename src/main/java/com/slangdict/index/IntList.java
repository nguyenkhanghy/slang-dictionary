package com.slangdict.index;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Danh sách int có thứ tự tăng dần, không trùng, gọn nhẹ (không boxing) dùng làm posting list.
 * Mọi thao tác ghi đều giữ bất biến "tăng dần, không trùng".
 */
final class IntList {

    private int[] data = new int[2];
    private int size;

    /** Chèn giá trị giữ đúng thứ tự; bỏ qua nếu đã có. Trường hợp thường gặp (giá trị lớn nhất) là O(1). */
    void insert(int value) {
        if (size == 0 || data[size - 1] < value) {
            ensureCapacity();
            data[size++] = value;
            return;
        }
        int pos = Arrays.binarySearch(data, 0, size, value);
        if (pos >= 0) {
            return; // đã có
        }
        pos = -pos - 1;
        ensureCapacity();
        System.arraycopy(data, pos, data, pos + 1, size - pos);
        data[pos] = value;
        size++;
    }

    /** Xóa giá trị nếu có. @return true nếu đã xóa. */
    boolean remove(int value) {
        int pos = Arrays.binarySearch(data, 0, size, value);
        if (pos < 0) {
            return false;
        }
        System.arraycopy(data, pos + 1, data, pos, size - pos - 1);
        size--;
        return true;
    }

    /** Giảm 1 mọi giá trị lớn hơn {@code removedId} (dùng sau khi xóa một entry và dồn id). */
    void decrementAbove(int removedId) {
        if (size == 0 || data[size - 1] <= removedId) {
            return;
        }
        int pos = Arrays.binarySearch(data, 0, size, removedId);
        int start = pos >= 0 ? pos + 1 : -pos - 1; // phần tử đầu tiên > removedId
        for (int i = start; i < size; i++) {
            data[i]--;
        }
    }

    boolean isEmpty() {
        return size == 0;
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
        int previous = -1;
        for (int i = 0; i < n; i++) {
            int v = in.readInt();
            if (v < 0 || v >= bound || v <= previous) {
                throw new IOException("Corrupt posting id: " + v);
            }
            list.data[i] = v;
            previous = v;
        }
        list.size = n;
        return list;
    }

    private void ensureCapacity() {
        if (size == data.length) {
            data = Arrays.copyOf(data, size * 2);
        }
    }
}
