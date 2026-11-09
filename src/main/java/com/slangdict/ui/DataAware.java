package com.slangdict.ui;

/** Trang giữ dữ liệu lấy từ từ điển (kết quả tìm, từ ngẫu nhiên...) cần bỏ/tải lại khi từ điển đổi. */
interface DataAware {

    /** Từ điển vừa thay đổi (thêm/sửa/xóa/reset) ở nơi khác. Gọi trên EDT. */
    void dataChanged();
}
