package com.slangdict.repository;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.zip.CRC32;

import com.slangdict.index.SlangIndex;
import com.slangdict.util.LogSupport;

/**
 * Lưu/nạp index ra file nhị phân để các lần chạy sau KHÔNG phải index lại (yêu cầu R4).
 *
 * <p>Cấu trúc file: {@code MAGIC, VERSION, kích thước file dữ liệu, CRC32 file dữ liệu, <body SlangIndex>, END_MAGIC}.
 * Index chỉ được dùng khi kích thước + CRC32 của file dữ liệu ({@code slang.current.txt}) còn khớp, nghĩa là
 * file txt chưa bị sửa ngoài ứng dụng. Dùng CRC32 thay vì ngày sửa file vì ngày sửa bị đổi khi copy/giải nén zip.
 * File thiếu/hỏng/lệch phiên bản -> trả {@link Optional#empty()} (caller tự build lại), không bao giờ crash.
 */
public final class IndexStore {

    private static final Logger LOG = Logger.getLogger(IndexStore.class.getName());

    private static final int MAGIC = 0x534C4958;      // "SLIX"
    private static final int END_MAGIC = 0x454E4421;  // "END!"
    private static final int VERSION = 1;

    private IndexStore() {
    }

    /** Nạp index từ {@code indexFile} nếu còn hợp lệ so với {@code dataFile}; thiếu/hỏng/lệch → {@link Optional#empty()}, không ném lỗi. */
    public static Optional<SlangIndex> load(Path indexFile, Path dataFile) {
        if (!Files.isRegularFile(indexFile)) {
            return Optional.empty();
        }
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(indexFile), 1 << 16))) {
            if (in.readInt() != MAGIC || in.readInt() != VERSION) {
                LOG.warning("Index file has unknown format/version, will rebuild");
                return Optional.empty();
            }
            long size = in.readLong();
            long crc = in.readLong();
            if (size != Files.size(dataFile) || crc != crc32(dataFile)) {
                LOG.info("Data file changed since index was saved, will rebuild");
                return Optional.empty();
            }
            SlangIndex index = SlangIndex.readFrom(in);
            if (in.readInt() != END_MAGIC) {
                LOG.warning("Index file is truncated, will rebuild");
                return Optional.empty();
            }
            return Optional.of(index);
        } catch (IOException | RuntimeException e) {
            LogSupport.warn(LOG, "Cannot load index file, will rebuild", e);
            return Optional.empty();
        }
    }

    /** Ghi index (kèm kích thước + CRC32 của {@code dataFile}) bằng {@link AtomicFiles}. */
    public static void save(SlangIndex index, Path indexFile, Path dataFile) throws IOException {
        long size = Files.size(dataFile);
        long crc = crc32(dataFile);
        AtomicFiles.write(indexFile, out -> {
            DataOutputStream data = new DataOutputStream(out);
            data.writeInt(MAGIC);
            data.writeInt(VERSION);
            data.writeLong(size);
            data.writeLong(crc);
            index.writeTo(data);
            data.writeInt(END_MAGIC);
            data.flush();
        });
    }

    private static long crc32(Path file) throws IOException {
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[1 << 16];
        try (InputStream in = Files.newInputStream(file)) {
            int n;
            while ((n = in.read(buffer)) > 0) {
                crc.update(buffer, 0, n);
            }
        }
        return crc.getValue();
    }
}