package com.example.afinal;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity {

    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1; // Mã yêu cầu quyền vị trí
    private SearchView searchView;
    private Button btnGetRoute;
    private GeoPoint destinationPoint; // Địa điểm đã tìm kiếm

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.maps);

        mapView = findViewById(R.id.mapView);
        searchView = findViewById(R.id.searchView);
        btnGetRoute = findViewById(R.id.btnGetRoute);

        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Khởi tạo FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Kiểm tra quyền vị trí
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Nếu chưa có quyền, yêu cầu quyền
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Nếu đã có quyền, lấy vị trí hiện tại
            getCurrentLocation();
        }

        // Xử lý sự kiện tìm kiếm
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Tìm kiếm địa điểm và di chuyển bản đồ đến đó
                searchLocation(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        // Xử lý sự kiện nút dẫn đường
        btnGetRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (destinationPoint != null) {
                    getRouteToDestination(destinationPoint);
                } else {
                    Toast.makeText(MapActivity.this, "Vui lòng tìm kiếm địa điểm trước", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    // Xử lý kết quả trả về từ yêu cầu quyền vị trí
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Nếu được cấp quyền, lấy vị trí hiện tại
                getCurrentLocation();
            } else {
                // Nếu không được cấp quyền, thông báo người dùng
                Toast.makeText(this, "Cần cấp quyền truy cập vị trí để sử dụng tính năng này", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                Location location = task.getResult();
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();

                                // Kiểm tra nếu vị trí hợp lệ
                                if (latitude != 0.0 && longitude != 0.0) {
                                    // Di chuyển bản đồ đến vị trí người dùng
                                    IMapController mapController = mapView.getController();
                                    GeoPoint userLocation = new GeoPoint(latitude, longitude);
                                    mapController.setCenter(userLocation);
                                    mapController.setZoom(15); // Thiết lập mức zoom

                                    // Thêm marker vào vị trí người dùng
                                    Marker userMarker = new Marker(mapView);
                                    userMarker.setPosition(userLocation);
                                    userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                    userMarker.setTitle("Vị trí hiện tại");
                                    mapView.getOverlays().add(userMarker);
                                } else {
                                    Log.e("MapActivity", "Vị trí người dùng không hợp lệ");
                                    Toast.makeText(MapActivity.this, "Không thể xác định vị trí của bạn", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.e("MapActivity", "Không thể lấy vị trí hiện tại");
                                Toast.makeText(MapActivity.this, "Không thể lấy vị trí của bạn", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    // Tìm kiếm và di chuyển bản đồ đến địa điểm tìm thấy
    private void searchLocation(String query) {
        // Ở đây bạn có thể sử dụng API tìm kiếm địa điểm như Nominatim hoặc Google Places API
        // Ví dụ giả lập với một địa chỉ có sẵn: Oxford Street, London
        GeoPoint geoPoint = new GeoPoint(51.5145, -0.1447); // Oxford Street, London (ví dụ)
        destinationPoint = geoPoint;

        // Di chuyển bản đồ đến địa điểm tìm thấy
        IMapController mapController = mapView.getController();
        mapController.setCenter(geoPoint);
        mapController.setZoom(15); // Thiết lập mức zoom

        // Thêm marker vào địa điểm tìm thấy
        Marker marker = new Marker(mapView);
        marker.setPosition(geoPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(query);
        mapView.getOverlays().add(marker);
    }

    // Tính toán lộ trình đến địa điểm đã tìm thấy
    private void getRouteToDestination(GeoPoint destination) {
        // Vị trí hiện tại của người dùng
        double currentLatitude = mapView.getMapCenter().getLatitude();
        double currentLongitude = mapView.getMapCenter().getLongitude();

        // Tạo URL API OSRM với điểm xuất phát và điểm đến
        String urlString = String.format(
                "http://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=polyline",
                currentLongitude, currentLatitude, destination.getLongitude(), destination.getLatitude()
        );

        // Gọi API và nhận dữ liệu trả về
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Phân tích JSON và vẽ polyline
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray routes = jsonResponse.getJSONArray("routes");
                    if (routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);
                        String encodedPolyline = route.getJSONObject("geometry").getString("coordinates");
                        List<GeoPoint> routePoints = decodePolyline(encodedPolyline);

                        // Vẽ polyline lên bản đồ
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Polyline polyline = new Polyline(mapView);
                                for (GeoPoint point : routePoints) {
                                    polyline.addPoint(point);
                                }
                                polyline.setColor(0xFF0000FF);  // Màu đỏ cho đường đi
                                mapView.getOverlays().add(polyline);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MapActivity.this, "Không thể tính toán lộ trình", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    // Giải mã polyline từ API OSRM
    private List<GeoPoint> decodePolyline(String encodedPolyline) {
        List<GeoPoint> points = new ArrayList<>();
        int index = 0, lat = 0, lng = 0;

        while (index < encodedPolyline.length()) {
            int shift = 0;
            int result = 0;
            while (true) {
                int byteValue = encodedPolyline.charAt(index++) - 63;
                result |= (byteValue & 0x1f) << shift;
                shift += 5;
                if (byteValue < 0x20) break;
            }
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            shift = 0;
            result = 0;
            while (true) {
                int byteValue = encodedPolyline.charAt(index++) - 63;
                result |= (byteValue & 0x1f) << shift;
                shift += 5;
                if (byteValue < 0x20) break;
            }
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            points.add(new GeoPoint((double) lat / 1E5, (double) lng / 1E5));
        }

        return points;
    }
}
