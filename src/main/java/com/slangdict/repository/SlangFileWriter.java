package com.slangdict.repository;

import com.slangdict.model.SlangWord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * Ghi danh sách slang ra file UTF-8, mỗi entry một dòng ({@code SLANG`meaning1| meaning2}), xuống dòng LF.
 * Ghi an toàn qua {@link AtomicFiles}. Không ghi dòng header.
 */
public final class SlangFileWriter {

    private SlangFileWriter() {
    }

    public static void write(Path file, List<SlangWord> words) throws IOException {
        AtomicFiles.write(file, out -> {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), 1 << 16);
            for (SlangWord word : words) {
                writer.write(word.toLine());
                writer.write('\n');
            }
            writer.flush();
        });
    }
}
