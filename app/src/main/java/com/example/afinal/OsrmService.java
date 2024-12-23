package com.example.afinal;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface OsrmService {
    // Lệnh GET để tìm đường giữa 2 điểm
    @GET("route/v1/driving/{startLong},{startLat};{endLong},{endLat}?overview=full")
    Call<OsrmResponse> getRoute(
            @Path("startLong") double startLong,
            @Path("startLat") double startLat,
            @Path("endLong") double endLong,
            @Path("endLat") double endLat
    );
}