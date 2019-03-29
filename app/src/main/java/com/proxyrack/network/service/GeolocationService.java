package com.proxyrack.network.service;

import com.proxyrack.network.model.Geolocation;
import io.reactivex.Single;
import retrofit2.http.GET;

public interface GeolocationService {

    @GET("json")
    Single<Geolocation> getLocation();
}
