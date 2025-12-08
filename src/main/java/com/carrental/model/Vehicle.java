package com.carrental.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "model_id", nullable = false)
    private VehicleModel model;

    @ManyToOne
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "license_plate", nullable = false, unique = true)
    private String licensePlate;

    @Column(name = "daily_rate", nullable = false)
    private BigDecimal dailyRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    @Column(name = "deposit_amount", nullable = false)
    private BigDecimal depositAmount;

    @Column(name = "color")
    private String color;

    // Lưu trữ danh sách ảnh dưới dạng JSON array: ["image1.jpg", "image2.jpg"]
    @Column(columnDefinition = "json")
    private String images;

    // Trạng thái của xe - Map với database values
    public enum VehicleStatus {
        Available,    // Có sẵn
        Rented,       // Đang được thuê
        Maintenance;  // Đang bảo trì

        // Thêm constant để dễ dùng
        public static final VehicleStatus AVAILABLE = Available;
        public static final VehicleStatus RENTED = Rented;
        public static final VehicleStatus MAINTENANCE = Maintenance;
    }

    // ========== GETTERS VÀ SETTERS ==========
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VehicleModel getModel() {
        return model;
    }

    public void setModel(VehicleModel model) {
        this.model = model;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(BigDecimal dailyRate) {
        this.dailyRate = dailyRate;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    /**
     * Lấy URL ảnh đầu tiên từ mảng JSON images
     * Trả về ảnh placeholder nếu không có ảnh nào
     * Hỗ trợ cả định dạng JSON array và tên file đơn giản
     *
     * @return Đường dẫn ảnh đầu tiên hoặc ảnh placeholder
     */
    public String getFirstImageUrl() {
        // Nếu không có ảnh, trả về ảnh placeholder
        if (images == null || images.trim().isEmpty()) {
            return "/images/vehicle-placeholder.jpg";
        }

        String trimmedImages = images.trim();

        // Nếu là mảng JSON, parse nó
        if (trimmedImages.startsWith("[")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<String> imageList = mapper.readValue(trimmedImages, new TypeReference<List<String>>() {});
                if (imageList != null && !imageList.isEmpty()) {
                    // Lấy ảnh đầu tiên trong danh sách
                    String imageUrl = imageList.get(0).trim();
                    return formatImageUrl(imageUrl);
                }
            } catch (Exception e) {
                // Nếu parse JSON thất bại, thử trích xuất tên file thủ công
                return extractImageFromJson(trimmedImages);
            }
        } else {
            // Nếu chỉ là tên file hoặc chuỗi đơn giản, sử dụng trực tiếp
            return formatImageUrl(trimmedImages);
        }

        return "/images/vehicle-placeholder.jpg";
    }
    
    /**
     * Định dạng URL ảnh để đảm bảo nó trỏ đến thư mục /images/
     * 
     * @param imageUrl URL ảnh cần định dạng
     * @return URL ảnh đã được định dạng đúng
     */
    private String formatImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return "/images/vehicle-placeholder.jpg";
        }
        
        imageUrl = imageUrl.trim();
        
        // Nếu là URL HTTP đầy đủ, trả về như cũ
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        
        // Loại bỏ dấu ngoặc kép nếu có
        imageUrl = imageUrl.replaceAll("^[\"']|[\"']$", "");
        
        // Nếu đã bắt đầu bằng /images/, trả về như cũ
        if (imageUrl.startsWith("/images/")) {
            return imageUrl;
        }
        
        // Nếu bắt đầu bằng /, coi như là đường dẫn đầy đủ
        if (imageUrl.startsWith("/")) {
            return imageUrl;
        }
        
        // Ngược lại, coi như chỉ là tên file và thêm tiền tố /images/
        return "/images/" + imageUrl;
    }
    
    /**
     * Thử trích xuất tên file ảnh từ chuỗi JSON thủ công
     * Sử dụng khi parse JSON thất bại
     * 
     * @param json Chuỗi JSON chứa thông tin ảnh
     * @return URL ảnh đã được định dạng hoặc placeholder
     */
    private String extractImageFromJson(String json) {
        // Sử dụng regex đơn giản để trích xuất giá trị string đầu tiên từ mảng JSON
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"([^\"]+)\"");
        java.util.regex.Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String imageUrl = matcher.group(1);
            return formatImageUrl(imageUrl);
        }
        return "/images/vehicle-placeholder.jpg";
    }

    // ========== EQUALS & HASHCODE ==========
    // Cần thiết để stream().distinct() hoạt động đúng
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vehicle vehicle = (Vehicle) o;
        return Objects.equals(id, vehicle.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
