# Kế hoạch Tối ưu Logic Định hướng Thước (Orientation Logic)

Mục tiêu: Làm cho việc xoay và cố định thước trở nên thông minh, dễ dùng và nhất quán giữa các chế độ.

## 1. Phân tích & Đề xuất Logic mới

### Chế độ 1 Thước (Giữ nguyên - Đã ổn)
*   **Tự do:** Xoay 360 độ bằng 2 ngón tay.
*   **Cố định Ngang:** Luôn nằm ở 0 độ.
*   **Cố định Dọc:** Luôn nằm ở 90 độ.

### Chế độ 2 Thước - Ghép đôi (L-Shape)
*   **Cho phép xoay (Free):** Cả cụm chữ "L" xoay cùng nhau khi dùng 2 ngón tay trên bất kỳ cây thước nào. Rất hữu ích khi cần đo các góc chéo của vật thể.
*   **Cố định (Fixed):** Khóa cứng cụm chữ "L" vào khung trục tọa độ (Thước ngang 0°, Thước dọc 90°). Người dùng chỉ có thể di chuyển, không thể xoay.

### Chế độ 2 Thước - Tách rời (Independent)
*   **Cho phép xoay (Free):** Từng cây thước xoay độc lập. Chạm vào cây nào cây đó xoay.
*   **Cố định (Fixed):** Đây là logic thông minh tôi đề xuất:
    *   Thước Ngang (H) sẽ tự động khóa về 0°.
    *   Thước Dọc (V) sẽ tự động khóa về 90°.
    *   *Mục đích:* Tạo ra bộ thước căn lề chuẩn xác mà không cần căn chỉnh tay.

## 2. Thay đổi về UI trong App
*   Cập nhật phần "CỐ ĐỊNH HƯỚNG" trong `MainActivity.kt`:
    *   Nếu chọn **2 thước**: Đổi nhãn từ "Nằm ngang/Thẳng đứng" thành **"Khóa góc (0° & 90°)"** và **"Xoay tự do"**.

## 3. Thay đổi về Code (RulerComponent.kt)
*   Cập nhật hàm `normalizeState` để ép góc xoay dựa trên số lượng thước và chế độ ghép đôi.
*   Cập nhật logic `rotateForMode` để xử lý xoay đồng bộ hoặc độc lập.

## 4. Các bước thực hiện
1.  [MODIFY] [RulerState.kt](file:///D:/AndroidStudioProjects/Thuocdo_manhinh/app/src/main/java/com/quangthe/thuocdo/model/RulerState.kt): Cập nhật mô tả các hằng số định hướng.
2.  [MODIFY] [MainActivity.kt](file:///D:/AndroidStudioProjects/Thuocdo_manhinh/app/src/main/java/com/quangthe/thuocdo/MainActivity.kt): Cập nhật giao diện cài đặt linh hoạt theo số lượng thước.
3.  [MODIFY] [RulerComponent.kt](file:///D:/AndroidStudioProjects/Thuocdo_manhinh/app/src/main/java/com/quangthe/thuocdo/ui/overlay/RulerComponent.kt): Triển khai logic khóa góc thông minh.
